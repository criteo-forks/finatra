package com.twitter.finatra.jackson.tests

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonNaming}
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.BigDecimalDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ValueNode
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.twitter.finatra.jackson.{CarMake, CarMakeEnum, TestInjectableValue}
import com.twitter.finatra.jackson.caseclass.SerdeLogging
import com.twitter.finatra.json.annotations.JsonCamelCase
import com.twitter.finatra.validation.{
  CommonMethodValidations,
  ErrorCode,
  MethodValidation,
  ValidationResult
}
import com.twitter.finatra.validation.constraints._
import com.twitter.inject.domain.WrappedValue
import com.twitter.util.Time
import com.twitter.{util => ctu}
import javax.inject.Inject
import org.joda.time.DateTime
import scala.annotation.meta.param
import scala.math.BigDecimal.RoundingMode

object Weekday extends Enumeration {
  type Weekday = Value
  val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
}
class WeekdayType extends TypeReference[Weekday.type]
object Month extends Enumeration {
  type Month = Value
  val Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec = Value
}
class MonthType extends TypeReference[Month.type]
case class BasicDate(
  @JsonScalaEnumeration(classOf[MonthType]) month: Month.Month,
  day: Int,
  year: Int,
  @JsonScalaEnumeration(classOf[WeekdayType]) weekday: Weekday.Weekday)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new Type(value = classOf[Rectangle], name = "rectangle"),
    new Type(value = classOf[Circle], name = "circle")
  )
)
sealed trait Shape
case class Rectangle(@Min(0) width: Int, @Min(0) height: Int) extends Shape
case class Circle(@Min(0) radius: Int) extends Shape
case class View(shapes: Seq[Shape])
case class OptionalView(shapes: Seq[Shape], optional: Option[Shape])

case class TestJsonCreator(int: Int)
case class TestJsonCreator2(ints: Seq[Int], default: String = "Hello, World")

object TestJsonCreator {
  @JsonCreator
  def apply(s: String): TestJsonCreator = TestJsonCreator(s.toInt)
}

object TestJsonCreator2 {
  @JsonCreator
  def apply(strings: Seq[String]): TestJsonCreator2 = TestJsonCreator2(strings.map(_.toInt))
}

case class CaseClassWithMultipleConstructors(number1: Long, number2: Long, number3: Long) {
  def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
    this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
  }
}

case class CaseClassWithMultipleConstructorsAnnotated(number1: Long, number2: Long, number3: Long) {
  @JsonCreator
  def this(numberAsString1: String, numberAsString2: String, numberAsString3: String) {
    this(numberAsString1.toLong, numberAsString2.toLong, numberAsString3.toLong)
  }
}

case class TimeWithFormat(@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX") when: Time)

/* Note: the decoder automatically changes "_i" to "i" for de/serialization:
 * See CaseClassField#jsonNameForField */
trait Aumly { @JsonProperty("i") def _i: Int; @JsonProperty("j") def _j: String }
case class Aum(_i: Int, _j: String) extends Aumly

trait Bar {
  @JsonProperty("helloWorld") @TestInjectableValue(value = "accept")
  def hello: String
}
case class FooBar(hello: String) extends Bar

trait Baz extends Bar {
  @JsonProperty("goodbyeWorld")
  def hello: String
}
case class FooBaz(hello: String) extends Baz

trait BarBaz {
  @JsonProperty("goodbye")
  def hello: String
}
case class FooBarBaz(hello: String)
    extends BarBaz
    with Bar // will end up with BarBaz @JsonProperty value as trait linearization is "right-to-left"

trait Loadable {
  @JsonProperty("url")
  def uri: String
}
abstract class Resource {
  @JsonProperty("resource")
  def uri: String
}
case class File(@JsonProperty("file") uri: String) extends Resource
case class Folder(@JsonProperty("folder") uri: String) extends Resource

abstract class LoadableResource extends Loadable {
  @JsonProperty("resource")
  override def uri: String
}
case class LoadableFile(@JsonProperty("file") uri: String) extends LoadableResource
case class LoadableFolder(@JsonProperty("folder") uri: String) extends LoadableResource

trait TestTrait {
  @JsonProperty("oldness")
  def age: Int
  @NotEmpty
  def name: String
}
@JsonCamelCase
case class TestTraitImpl(
  @JsonProperty("ageness") age: Int, // should override inherited annotation from trait
  @TestInjectableValue name: String, // should have two annotations, one from trait and one here
  @TestInjectableValue dateTime: DateTime,
  @JsonProperty foo: String,
  @JsonDeserialize(contentAs = classOf[BigDecimal], using = classOf[BigDecimalDeserializer])
  double: BigDecimal,
  @JsonIgnore ignoreMe: String)
    extends TestTrait {

  lazy val testFoo: String = "foo"
  lazy val testBar: String = "bar"
}

sealed trait CarType {
  @JsonValue
  def toJson: String
}
object Volvo extends CarType {
  override def toJson: String = "volvo"
}
object Audi extends CarType {
  override def toJson: String = "audi"
}
object Volkswagen extends CarType {
  override def toJson: String = "vw"
}

case class Vehicle(vin: String, `type`: CarType)

sealed trait ZeroOrOne
object Zero extends ZeroOrOne
object One extends ZeroOrOne
object Two extends ZeroOrOne

case class CaseClassWithZeroOrOne(id: ZeroOrOne)

case class CaseClass(id: Long, name: String)

case class SimpleClassWithInjection(@TestInjectableValue(value = "accept") hello: String)

@JsonIgnoreProperties(ignoreUnknown = false)
case class StrictCaseClass(id: Long, name: String)

@JsonIgnoreProperties(ignoreUnknown = true)
case class LooseCaseClass(id: Long, name: String)

case class CaseClassWithLazyVal(id: Long) {
  lazy val woo = "yeah"
}

case class GenericTestCaseClass[T](data: T)

case class Page[T](data: List[T], pageSize: Int, next: Option[Long], previous: Option[Long])

case class CaseClassWithOptionalGeneric[T](inside: Option[GenericTestCaseClass[T]])

case class CaseClassWithTypes[T, U](first: T, second: U)

case class CaseClassWithMapTypes[T, U](data: Map[T, U])

case class CaseClassWithManyTypes[R, S, T](one: R, two: S, three: T)

case class CaseClassIgnoredFieldInConstructorNoDefault(
  @JsonIgnore id: Long,
  name: String,
  description: String)

case class CaseClassIgnoredFieldInConstructorWithDefault(
  @JsonIgnore id: Long = 42L,
  name: String,
  description: String)

case class CaseClassWithIgnoredField(id: Long) {
  @JsonIgnore
  val ignoreMe = "Foo"
}

@JsonIgnoreProperties(Array("ignore_me", "feh"))
case class CaseClassWithIgnoredFieldsMatchAfterToSnakeCase(id: Long) {
  val ignoreMe = "Foo"
  val feh = "blah"
}

@JsonIgnoreProperties(Array("ignore_me", "feh"))
case class CaseClassWithIgnoredFieldsExactMatch(id: Long) {
  val ignore_me = "Foo"
  val feh = "blah"
}

case class CaseClassWithTransientField(id: Long) {
  @transient
  val lol = "asdf"
}

case class CaseClassWithLazyField(id: Long) {
  @JsonIgnore lazy val lol = "asdf"
}

case class CaseClassWithOverloadedField(id: Long) {
  def id(prefix: String): String = prefix + id
}

@JsonIgnoreProperties(ignoreUnknown = false)
case class StrictCaseClassWithOption(value: Option[String] = None)

case class CaseClassWithOption(value: Option[String] = None)

case class CaseClassWithJsonNode(value: JsonNode)

case class CaseClassWithAllTypes(
  map: Map[String, String],
  set: Set[Int],
  string: String,
  list: List[Int],
  seq: Seq[Int],
  indexedSeq: IndexedSeq[Int],
  vector: Vector[Int],
  bigDecimal: BigDecimal,
  bigInt: Int, //TODO: BigInt,
  int: Int,
  long: Long,
  char: Char,
  bool: Boolean,
  short: Short,
  byte: Byte,
  float: Float,
  double: Double,
  any: Any,
  anyRef: AnyRef,
  intMap: Map[Int, Int] = Map(),
  longMap: Map[Long, Long] = Map())

case class CaseClassWithException() {
  throw JsonMappingException.from(null.asInstanceOf[JsonParser], "Oops!!!")
}

object OuterObject {

  case class NestedCaseClass(id: Long)

  object InnerObject {

    case class SuperNestedCaseClass(id: Long)
  }
}

case class CaseClassWithSnakeCase(oneThing: String, twoThing: String)

case class CaseClassWithArrays(
  one: String,
  two: Array[String],
  three: Array[Int],
  four: Array[Long],
  five: Array[Char],
  bools: Array[Boolean],
  bytes: Array[Byte],
  doubles: Array[Double],
  floats: Array[Float])

case class CaseClassWithArrayLong(array: Array[Long])

case class CaseClassWithArrayListOfIntegers(arraylist: java.util.ArrayList[java.lang.Integer])

case class CaseClassWithArrayBoolean(array: Array[Boolean])

case class CaseClassWithArrayWrappedValueLong(array: Array[WrappedValueLong])

case class CaseClassWithSeqLong(seq: Seq[Long])

case class CaseClassWithSeqWrappedValueLong(seq: Seq[WrappedValueLong])

case class CaseClassWithValidation(@Min(1) value: Long)

case class CaseClassWithSeqOfCaseClassWithValidation(seq: Seq[CaseClassWithValidation])

case class WrappedValueLongWithValidation(@Min(1) value: Long) extends WrappedValue[Long]

case class CaseClassWithSeqWrappedValueLongWithValidation(seq: Seq[WrappedValueLongWithValidation])

case class Foo(name: String)

case class CaseClassCharacter(c: Char)

case class Car(
  id: Long,
  make: CarMake,
  model: String,
  @Min(2000) year: Int,
  owners: Seq[Person],
  @Min(0) numDoors: Int = 4,
  manual: Boolean = false,
  ownershipStart: DateTime = DateTime.now,
  ownershipEnd: DateTime = DateTime.now.plusYears(1),
  warrantyStart: Option[DateTime] = None,
  warrantyEnd: Option[DateTime] = None,
  passengers: Seq[Person] = Seq()) {

  @MethodValidation
  def validateId: ValidationResult = {
    ValidationResult.validate(id % 2 == 1, "id may not be even")
  }

  @MethodValidation
  def validateYearBeforeNow: ValidationResult = {
    val thisYear = new DateTime().getYear
    val yearMoreThanOneYearInFuture: Boolean =
      if (year > thisYear) { (year - thisYear) > 1 }
      else false
    ValidationResult.validateNot(
      yearMoreThanOneYearInFuture,
      "Model year can be at most one year newer."
    )
  }

  @MethodValidation(fields = Array("ownershipEnd"))
  def ownershipTimesValid: ValidationResult = {
    CommonMethodValidations.validateTimeRange(
      ownershipStart,
      ownershipEnd,
      "ownershipStart",
      "ownershipEnd"
    )
  }

  @MethodValidation(fields = Array("warrantyStart", "warrantyEnd"))
  def warrantyTimeValid: ValidationResult = {
    CommonMethodValidations.validateTimeRange(
      warrantyStart,
      warrantyEnd,
      "warrantyStart",
      "warrantyEnd"
    )
  }
}

case class PersonWithLogging(
  id: Int,
  name: String,
  age: Option[Int],
  age_with_default: Option[Int] = None,
  nickname: String = "unknown")
    extends SerdeLogging

case class PersonWithDottedName(id: Int, @JsonProperty("name.last") lastName: String)

case class SimplePerson(name: String)

case class PersonWithThings(
  id: Int,
  name: String,
  age: Option[Int],
  @Size(min = 1, max = 10) things: Map[String, Things])

case class Things(@Size(min = 1, max = 2) names: Seq[String])

@JsonCamelCase
case class CamelCaseSimplePerson(myName: String)

case class CamelCaseSimplePersonNoAnnotation(myName: String)

case class CaseClassWithMap(map: Map[String, String])

case class CaseClassWithSortedMap(sortedMap: scala.collection.SortedMap[String, Int])

case class CaseClassWithSetOfLongs(set: Set[Long])

case class CaseClassWithSeqOfLongs(seq: Seq[Long])

case class CaseClassWithNestedSeqLong(
  seqClass: CaseClassWithSeqLong,
  setClass: CaseClassWithSetOfLongs)

case class Blah(foo: String)

case class TestIdStringWrapper(id: String) extends WrappedValue[String]

case class ObjWithTestId(id: TestIdStringWrapper)

object Obj {

  case class NestedCaseClassInObject(id: String)

  case class NestedCaseClassInObjectWithNestedCaseClassInObjectParam(
    nested: NestedCaseClassInObject)

}

class TypeAndCompanion
object TypeAndCompanion {
  case class NestedCaseClassInCompanion(id: String)
}

case class WrappedValueInt(value: Int) extends WrappedValue[Int]

case class WrappedValueLong(value: Long) extends WrappedValue[Long]

case class WrappedValueString(value: String) extends WrappedValue[String]

case class WrappedValueIntInObj(foo: WrappedValueInt)

case class WrappedValueStringInObj(foo: WrappedValueString)

case class WrappedValueLongInObj(foo: WrappedValueLong)

case class CaseClassWithVal(name: String) {
  val `type`: String = "person"
}

case class CaseClassWithEnum(name: String, make: CarMakeEnum)

case class CaseClassWithComplexEnums(
  name: String,
  make: CarMakeEnum,
  makeOpt: Option[CarMakeEnum],
  makeSeq: Seq[CarMakeEnum],
  makeSet: Set[CarMakeEnum])

case class CaseClassWithSeqEnum(enumSeq: Seq[CarMakeEnum])

case class CaseClassWithOptionEnum(enumOpt: Option[CarMakeEnum])

case class CaseClassWithDateTime(dateTime: DateTime)

case class CaseClassWithIntAndDateTime(
  @NotEmpty name: String,
  age: Int,
  age2: Int,
  age3: Int,
  dateTime: DateTime,
  dateTime2: DateTime,
  dateTime3: DateTime,
  dateTime4: DateTime,
  @NotEmpty dateTime5: Option[DateTime])

case class CaseClassWithTwitterUtilDuration(duration: ctu.Duration)

case class ClassWithFooClassInject(@Inject fooClass: FooClass)

case class ClassWithFooClassInjectAndDefault(@Inject fooClass: FooClass = FooClass("12345"))

case class ClassWithQueryParamDateTimeInject(@TestInjectableValue dateTime: DateTime)

case class CaseClassWithEscapedLong(`1-5`: Long)

case class CaseClassWithEscapedLongAndAnnotation(@Max(25) `1-5`: Long)

case class CaseClassWithEscapedString(`1-5`: String)

case class CaseClassWithEscapedNormalString(`a`: String)

case class UnicodeNameCaseClass(`winning-id`: Int, name: String)

case class TestEntityIdsResponse(entityIds: Seq[Long], previousCursor: String, nextCursor: String)

object TestEntityIdsResponseWithCompanion {
  val msg = "im the companion"
}

case class TestEntityIdsResponseWithCompanion(
  entityIds: Seq[Long],
  previousCursor: String,
  nextCursor: String)

case class WrappedValueStringMapObject(map: Map[WrappedValueString, String])

case class FooClass(id: String)

case class Group3(id: String) extends SerdeLogging

case class CaseClassWithNotEmptyValidation(@NotEmpty name: String, make: CarMakeEnum)

// InvalidConstraint is not provided by Finatra by default
case class CaseClassWithInvalidValidation(
  @(InvalidConstraint @param) name: String,
  make: CarMakeEnum)

case class NoConstructorArgs()

case class CaseClassWithBoolean(foo: Boolean)

case class CaseClassWithSeqBooleans(foos: Seq[Boolean])

case class CaseClassInjectStringWithDefault(@Inject string: String = "DefaultHello")

case class CaseClassInjectInt(@Inject age: Int)

case class CaseClassInjectOptionInt(@Inject age: Option[Int])

case class CaseClassInjectOptionString(@Inject string: Option[String])

case class CaseClassInjectString(@Inject string: String)

case class CaseClassTooManyInjectableAnnotations(@Inject @TestInjectableValue string: String)

case class CaseClassWithManyAnnotationsThatShouldProbablyBeAcceptable(
  @JsonIgnore @Inject string: String)

case class CaseClassWithManyAnnotationsFail(@JsonProperty("whatever") @Inject string: String)

case class CaseClassWithCustomDecimalFormat(
  @JsonDeserialize(using = classOf[MyBigDecimalDeserializer]) myBigDecimal: BigDecimal,
  @JsonDeserialize(using = classOf[MyBigDecimalDeserializer]) optMyBigDecimal: Option[BigDecimal])

case class CaseClassWithLongAndDeserializer(
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  long: Number)

case class CaseClassWithOptionLongAndDeserializer(
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  optLong: Option[Long])

case class CaseClassWithTwoConstructors(id: Long, @NotEmpty name: String) {
  def this(id: Long) = this(id, "New User")
}

case class CaseClassWithThreeConstructors(id: Long, @NotEmpty name: String) {
  def this(id: Long) = this(id, "New User")

  def this(name: String) = this(42, name)
}

case class Person(
  id: Int,
  @NotEmpty name: String,
  dob: Option[DateTime] = None,
  age: Option[Int],
  age_with_default: Option[Int] = None,
  nickname: String = "unknown",
  address: Option[Address] = None)

class MyBigDecimalDeserializer extends StdDeserializer[BigDecimal](classOf[BigDecimal]) {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): BigDecimal = {
    val jsonNode: ValueNode = jp.getCodec.readTree(jp)
    BigDecimal(jsonNode.asText).setScale(2, RoundingMode.HALF_UP)
  }

  override def getEmptyValue: BigDecimal = BigDecimal(0)
}

case class WithEmptyJsonProperty(@JsonProperty foo: String)

case class WithNonemptyJsonProperty(@JsonProperty("bar") foo: String)

case class WithoutJsonPropertyAnnotation(foo: String)

case class NamingStrategyJsonProperty(@JsonProperty longFieldName: String)

case class Address(
  @NotEmpty street: Option[String] = None,
  @NotEmpty city: String,
  @NotEmpty state: String) {

  @MethodValidation
  def validateState: ValidationResult =
    ValidationResult.validate(
      state == "CA" || state == "MD" || state == "WI",
      "state must be one of [CA, MD, WI]"
    )
}

trait CaseClassTrait {
  @JsonProperty("fedoras")
  @Size(min = 1, max = 2)
  def names: Seq[String]

  @Min(1L)
  def age: Int
}
case class CaseClassTraitImpl(names: Seq[String], @JsonProperty("oldness") age: Int)
    extends CaseClassTrait

package object internal {
  case class SimplePersonInPackageObject( // not recommended but used here for testing use case
    name: String = "default-name")

  case class SimplePersonInPackageObjectWithoutConstructorParams(
  ) // not recommended but used here for testing use case
}

case class LimiterProfile(id: Long, name: String)
case class LimiterProfiles(profiles: Option[Seq[LimiterProfile]] = None)

object LimiterProfiles {
  def apply(profiles: Seq[LimiterProfile]): LimiterProfiles = {
    if (profiles.isEmpty) {
      LimiterProfiles()
    } else {
      LimiterProfiles(Some(profiles))
    }
  }
}

case class CaseClassWithBoxedPrimitives(events: Integer, errors: Integer)

sealed trait ClusterRequest
case class AddClusterRequest(
  @Size(min = 0, max = 30) clusterName: String,
  @NotEmpty job: String,
  zone: String,
  environment: String,
  dtab: String,
  address: String,
  owners: String = "",
  dedicated: Boolean = true,
  enabled: Boolean = true,
  description: String = "")
    extends ClusterRequest {

  @MethodValidation(fields = Array("clusterName"))
  def validateClusterName: ValidationResult = {
    validateName(clusterName)
  }

  private def validateName(name: String): ValidationResult = {
    val regex = "[0-9a-zA-Z_\\-\\.>]+"
    ValidationResult.validate(
      name.matches(regex),
      s"$name is invalid. Only alphanumeric and special characters from (_,-,.,>) are allowed.",
      ErrorCode.PatternNotMatched(name, regex))
  }
}

case class Point(abscissa: Int, ordinate: Int) {
  def area: Int = abscissa * ordinate
}

case class Points(first: Point, second: Point)

trait PointMixin {
  @JsonProperty("x") @Min(0) @Max(100) def abscissa: Int
  @JsonProperty("y") @Min(0) @Max(100) def ordinate: Int
  @JsonIgnore def area: Int
}

@JsonIgnoreType
case class IgnoreMe(id: Long)

case class ContainsAnIgnoreTypeNoDefault(ignored: IgnoreMe, name: String, description: String)
case class ContainsAnIgnoreTypeWithDefault(
  ignored: IgnoreMe = IgnoreMe(42L),
  name: String,
  description: String)

object Views {
  class Public
  class Internal extends Public
}

case class Item(
  @JsonView(Array(classOf[Views.Public])) id: Long = 1L,
  @JsonView(Array(classOf[Views.Public])) name: String = "",
  @JsonView(Array(classOf[Views.Internal])) owner: String = "")

case class ItemSomeViews(
  @JsonView(Array(classOf[Views.Public])) id: Long = 1L,
  @JsonView(Array(classOf[Views.Public])) name: String = "",
  owner: String = "")

case class ItemNoDefaultForView(@JsonView(Array(classOf[Views.Public])) name: String)

@JsonNaming(classOf[PropertyNamingStrategy.KebabCaseStrategy])
case class CaseClassWithKebabCase(pleaseUseKebabCase: Boolean)

@JsonNaming(classOf[PropertyNamingStrategy.KebabCaseStrategy])
trait KebabCaseMixin

case class CaseClassShouldUseKebabCaseFromMixin(willThisGetTheRightCasing: Boolean)

@JsonNaming
case class UseDefaultNamingStrategy(thisFieldShouldUseDefaultPropertyNamingStrategy: Boolean)
