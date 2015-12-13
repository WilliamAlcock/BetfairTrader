package service.newTestService

import domain._
import org.joda.time.DateTime

trait OrderFactory {
  private var betId: Int = 0

  private def getBetId(): String = {
    betId += 1
    "SimBet_" + betId.toString
  }

  private def createLimitOrder(instruction: PlaceInstruction): Order = {
    Order(
      getBetId(),                                       // betId
      OrderType.LIMIT,                                  // orderType
      OrderStatus.EXECUTABLE,                           // status
      instruction.limitOrder.get.persistenceType,       // persistenceType
      instruction.side,                                 // side
      instruction.limitOrder.get.price,                 // price
      instruction.limitOrder.get.size,                  // size
      0,                                                // bsp liability
      DateTime.now(),                                   // placed Date
      0,                                                // avg Price Matched
      0,                                                // size Matched
      instruction.limitOrder.get.size,                  // size Remaining
      0,                                                // size Lapsed
      0,                                                // size Cancelled
      0                                                 // size Voided
    )
  }

  // TODO better handle these orders
  private def createLimitOnCloseOrder(instruction: PlaceInstruction): Order = {
    Order(
      getBetId(),                                       // betId
      OrderType.LIMIT_ON_CLOSE,                         // orderType
      OrderStatus.EXECUTABLE,                           // status
      PersistenceType.PERSIST,                          // persistenceType
      instruction.side,                                 // side
      instruction.limitOnCloseOrder.get.price,          // price
      instruction.limitOnCloseOrder.get.liability,      // size
      instruction.limitOnCloseOrder.get.liability,      // bsp liability
      DateTime.now(),                                   // placed Date
      0,                                                // avg Price Matched
      0,                                                // size Matched
      instruction.limitOnCloseOrder.get.liability,      // size Remaining
      0,                                                // size Lapsed
      0,                                                // size Cancelled
      0                                                 // size Voided
    )
  }

  // TODO better handle these orders
  private def createMarketOnCloseOrder(instruction: PlaceInstruction): Order = {
    Order(
      getBetId(),                                       // betId
      OrderType.MARKET_ON_CLOSE,                        // orderType
      OrderStatus.EXECUTABLE,                           // status
      PersistenceType.MARKET_ON_CLOSE,                  // persistenceType
      instruction.side,                                 // side
      0,                                                // price
      instruction.marketOnCloseOrder.get.liability,     // size
      instruction.marketOnCloseOrder.get.liability,     // bsp liability
      DateTime.now(),                                   // placed Date
      0,                                                // avg Price Matched
      0,                                                // size Matched
      instruction.limitOnCloseOrder.get.liability,      // size Remaining
      0,                                                // size Lapsed
      0,                                                // size Cancelled
      0                                                 // size Voided
    )
  }

  def createOrder(instruction: PlaceInstruction): Order = instruction.orderType match {
    case OrderType.LIMIT => createLimitOrder(instruction)
    case OrderType.LIMIT_ON_CLOSE => createLimitOnCloseOrder(instruction)
    case OrderType.MARKET_ON_CLOSE => createMarketOnCloseOrder(instruction)
  }
}

object OrderFactory extends OrderFactory {}

