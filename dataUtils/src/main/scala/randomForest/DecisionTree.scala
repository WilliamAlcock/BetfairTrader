package randomForest

import play.api.libs.json._

@SerialVersionUID(100L)
trait DecisionTree extends Serializable {
  def classify(data: List[Double]): String
}

trait DecisionTreeBuilder {
  def getDecisionTree(leafSize: Int, numberOfFeatures: Int, data: List[Instance]): DecisionTree
}

// all field names have been reduced to 1 char to decrease the size of the object when it is converted to json
object DecisionTree {
  implicit val readDecisionTree: Reads[DecisionTree] = {
    new Reads[DecisionTree] {
      def reads(json: JsValue) = (json \ "n").get match {
        case x: JsString if x.value == "I" => json.validate[InnerNode]
        case x: JsString if x.value == "L" => json.validate[LeafNode]
        case _ => JsError()
      }
    }
  }

  implicit val writeDecisionTree: Writes[DecisionTree] = {
    new Writes[DecisionTree] {
      def writes(d: DecisionTree) = d match {
        case x: InnerNode => Json.obj(
          "f"       -> JsNumber(x.f),
          "t"     -> JsNumber(x.t),
          "l"      -> Json.toJson(x.l),
          "r"     -> Json.toJson(x.r),
          "n"      -> JsString("I")
        )
        case x: LeafNode => Json.obj(
          "c"       -> Json.toJson(x.c),
          "n"      -> JsString("L")
        )
      }
    }
  }
}