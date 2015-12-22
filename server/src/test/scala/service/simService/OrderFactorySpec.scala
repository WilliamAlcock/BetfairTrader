package service.simService

import domain.Side
import org.scalatest.{Matchers, FlatSpec}
import TestHelpers._

class OrderFactorySpec extends FlatSpec with Matchers {

  def getNumber(betId: String): Int = betId.substring(7).toInt

  "OrderFactory" should "Increment betIds" in {
    object TestOrderFactory extends OrderFactory

    var lastId = getNumber(TestOrderFactory.createOrder(generatePlaceInstruction(Side.BACK,10.0, 20.0)).betId)
    1 to 100 foreach {x =>
      val nextId = getNumber(TestOrderFactory.createOrder(generatePlaceInstruction(Side.BACK,10.0, 20.0)).betId)
      nextId should be (lastId + 1)
      lastId = nextId
    }
  }

  // TODO Test transformation from instruction to order
}

