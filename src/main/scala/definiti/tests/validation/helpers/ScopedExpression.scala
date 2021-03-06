package definiti.tests.validation.helpers

import definiti.common.ast.{AliasType, ClassDefinition, DefinedType, Enum, Library, Location, NativeClassDefinition}
import definiti.tests.ast._
import definiti.tests.validation.ValidationContext
import definiti.tests.validation.helpers.ScopedExpression.TypeInfo

case class ScopedExpression[E <: Expression](
  expression: E,
  references: Map[String, TypeInfo],
  definedGenerics: Seq[String],
  generators: Seq[GeneratorMeta],
  library: Library
) {
  // Natural logic of types is to not be generators.
  private implicit def typeAsTypeInfo(typ: Type): TypeInfo = TypeInfo(typ, isGenerator = false)

  private val boolean = Type("Boolean", Seq.empty)
  private val number = Type("Number", Seq.empty)
  private val string = Type("String", Seq.empty)
  private val any = Type("Any", Seq.empty)

  def typeOfExpression: Type = process(expression).typ

  def isGeneratorExpression: Boolean = process(expression).isGenerator

  def location: Location = expression.location

  private def process(expression: Expression): TypeInfo = {
    expression match {
      case _: BooleanExpression => boolean
      case _: NumberExpression => number
      case _: StringExpression => string
      case generation: GenerationExpression =>
        generators.find(_.fullName == generation.name)
          .map[TypeInfo] { generator =>
            Types.replaceGenerics(generator.typ, generator.generics.zip(generation.generics).toMap)
          }
          .getOrElse(any)
      case structure: StructureExpression => structure.typ
      case methodCall: MethodCall =>
        val innerExpressionType = process(methodCall.inner)
        getFinalClassDefinition(innerExpressionType.typ.name) match {
          case Some(native: NativeClassDefinition) =>
            (for {
              method <- native.methods.find(_.name == methodCall.method)
              returnType = Types.typeReferenceToType(method.returnType)
              finalType = Types.replaceGenerics(returnType, method.genericTypes.zip(methodCall.generics).toMap)
            } yield {
              finalType: TypeInfo
            })
              .getOrElse(any)
          case _ => any
        }
      case attributeCall: AttributeCall =>
        val innerExpressionType = process(attributeCall.inner)
        getFinalClassDefinition(innerExpressionType.typ.name) match {
          case Some(native: NativeClassDefinition) =>
            native.attributes
              .find(_.name == attributeCall.attribute)
              .map(_.typeDeclaration)
              .map[TypeInfo](Types.typeDeclarationToType)
              .getOrElse(any)
          case Some(definedType: DefinedType) =>
            definedType.attributes
              .find(_.name == attributeCall.attribute)
              .map(_.typeDeclaration)
              .map[TypeInfo](Types.typeDeclarationToType)
              .getOrElse(any)
          case Some(enum: Enum) =>
            Type(enum.fullName)
          case _ => any
        }
      case reference: Reference =>
        references
          .get(reference.target)
          .orElse {
            getEnum(reference.target)
              .map(enum => TypeInfo(Type(enum.fullName), isGenerator = false))
          }
          .getOrElse(any)
      case condition: Condition =>
        val thenType = process(condition.thenCase)
        val elseType = process(condition.elseCase)
        if (thenType == elseType) {
          thenType
        } else {
          any
        }
      case binary: Binary =>
        if (BinaryOperator.isComputation(binary.operator)) number
        else boolean
    }
  }

  def getClassDefinition(typeName: String): Option[ClassDefinition] = {
    library.typesMap.get(typeName)
  }

  def getFinalClassDefinition(typeName: String): Option[ClassDefinition] = {
    getClassDefinition(typeName) match {
      case Some(aliasType: AliasType) => getFinalClassDefinition(aliasType.alias.typeName)
      case other => other
    }
  }

  def getEnum(enumName: String): Option[ClassDefinition] = {
    library.typesMap
      .get(enumName)
      .collect {
        case enum: Enum => enum
      }
  }

  def hasEnum(enumName: String): Boolean = {
    getEnum(enumName).isDefined
  }
}

object ScopedExpression {
  case class TypeInfo(typ: Type, isGenerator: Boolean)

  def apply[E <: Expression](expression: E, context: ValidationContext): ScopedExpression[E] = {
    new ScopedExpression(
      expression = expression,
      references = Map.empty,
      definedGenerics = Seq.empty,
      generators = context.generators,
      library = context.library
    )
  }

  def apply[E <: Expression](expression: E, generator: Generator, context: ValidationContext): ScopedExpression[E] = {
    new ScopedExpression(
      expression = expression,
      references = {
        generator.parameters.map { parameter =>
          if (parameter.isRest) {
            parameter.name -> TypeInfo(Type("List", Seq(parameter.typ)), isGenerator = parameter.isGen)
          } else {
            parameter.name -> TypeInfo(parameter.typ, isGenerator = parameter.isGen)
          }
        }.toMap
      },
      definedGenerics = generator.generics,
      generators = context.generators,
      library = context.library
    )
  }

  def apply[E <: Expression](expression: E, innerScope: ScopedExpression[_]): ScopedExpression[E] = {
    new ScopedExpression(
      expression = expression,
      references = innerScope.references,
      definedGenerics = innerScope.definedGenerics,
      generators = innerScope.generators,
      library = innerScope.library
    )
  }

  implicit class scopedGenerator(scopedExpression: ScopedExpression[GenerationExpression]) {
    def name: String = scopedExpression.expression.name

    def generics: Seq[Type] = scopedExpression.expression.generics

    def arguments: Seq[ScopedExpression[Expression]] = scopedExpression.expression.arguments.map(ScopedExpression(_, scopedExpression))
  }

  case class ScopedField(name: String, expression: ScopedExpression[Expression], location: Location)

  implicit class scopedStructure(scopedExpression: ScopedExpression[StructureExpression]) {
    def typ: Type = scopedExpression.expression.typ

    def fields: Seq[ScopedField] = scopedExpression.expression.fields.map(fieldToScopedField)

    private def fieldToScopedField(field: Field): ScopedField = {
      ScopedField(field.name, ScopedExpression(field.expression, scopedExpression), field.location)
    }
  }

  implicit class scopedMethodCall(scopedExpression: ScopedExpression[MethodCall]) {
    def inner: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.inner, scopedExpression)

    def method: String = scopedExpression.expression.method

    def arguments: Seq[ScopedExpression[Expression]] = scopedExpression.expression.arguments.map(ScopedExpression(_, scopedExpression))

    def generics: Seq[Type] = scopedExpression.expression.generics
  }

  implicit class scopedAttributeCall(scopedExpression: ScopedExpression[AttributeCall]) {
    def inner: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.inner, scopedExpression)

    def attribute: String = scopedExpression.expression.attribute
  }

  implicit class scopedReference(scopedExpression: ScopedExpression[Reference]) {
    def target: String = scopedExpression.expression.target
  }

  implicit class scopedCondition(scopedExpression: ScopedExpression[Condition]) {
    def condition: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.condition, scopedExpression)
    def thenCase: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.thenCase, scopedExpression)
    def elseCase: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.elseCase, scopedExpression)
  }

  implicit class scopedBinary(scopedExpression: ScopedExpression[Binary]) {
    def operator: BinaryOperator.Value = scopedExpression.expression.operator
    def left: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.left, scopedExpression)
    def right: ScopedExpression[Expression] = ScopedExpression(scopedExpression.expression.right, scopedExpression)
  }

}