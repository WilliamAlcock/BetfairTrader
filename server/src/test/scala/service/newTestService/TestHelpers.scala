package service.newTestService

import domain.OrderStatus.OrderStatus
import domain.PersistenceType.PersistenceType
import domain.Side.Side
import domain._
import org.joda.time.DateTime

class MockBackOrderBook extends OrderBook(Side.BACK)
class MockLayOrderBook extends OrderBook(Side.LAY)
class MockRunnerOrderBook extends RunnerOrderBook()

class MockPlaceInstructionReport extends PlaceInstructionReport(InstructionReportStatus.SUCCESS, None, TestHelpers.generatePlaceInstruction(Side.BACK, 1, 2), None, None, None, None)
class MockCancelInstructionReport extends CancelInstructionReport(InstructionReportStatus.SUCCESS, None, CancelInstruction("TestBetId", None), None, None)
class MockUpdateInstructionReport extends UpdateInstructionReport(InstructionReportStatus.SUCCESS, None, UpdateInstruction("TestBetId", PersistenceType.LAPSE))


object TestHelpers {
  // Helper function to generate orders
  def generateOrder(id: String,
                    price: Double,
                    size: Double,
                    side: Side,
                    status: OrderStatus = OrderStatus.EXECUTABLE,
                    persistenceType: PersistenceType = PersistenceType.LAPSE,
                    placedDate: DateTime = new DateTime(5467),
                    sizeMatched: Double = 0,
                    sizeRemaining: Option[Double] = None,
                    sizeCancelled: Double = 0): Order = {

    new Order(
      id,                                           // betId
      OrderType.LIMIT,                              // orderType
      status,                                       // orderStatus
      persistenceType,                              // persistenceType
      side,                                         // side
      price,                                        // price
      size,                                         // size
      0,                                            // bspLiability
      placedDate,                                   // placedDate
      0,                                            // avgPriceMatched
      sizeMatched,                                  // sizeMatched
      sizeRemaining.getOrElse(size),                // sizeRemaining
      0,                                            // sizeLapsed
      sizeCancelled,                                // sizeCancelled
      0                                             // sizeVoided
    )
  }

  // Helper function to generate PlaceInstruction
  def generatePlaceInstruction(side: Side, price: Double, size: Double): PlaceInstruction = {
    PlaceInstruction(OrderType.LIMIT, 1, 2, side, Some(LimitOrder(size, price, PersistenceType.LAPSE)), None, None)
  }

  // Helper function to generate failed cancel report
  def genFailedCancelReport(betId: String, size: Option[Double]) = CancelInstructionReport(
    InstructionReportStatus.FAILURE,
    Some(InstructionReportErrorCode.INVALID_BET_ID),
    CancelInstruction(betId, size),
    None,
    None
  )

  // Helper function to generate successful cancel report
  def genSuccessCancelReport(betId: String, size: Option[Double], sizeToCancel: Double) = CancelInstructionReport(
    InstructionReportStatus.SUCCESS,
    None,
    CancelInstruction(betId, size),
    Some(sizeToCancel),
    Some(DateTime.now())
  )

  // Helper function to generate failed cancel report
  def genFailedUpdateReport(betId: String, newPersistenceType: PersistenceType) = UpdateInstructionReport(
    InstructionReportStatus.FAILURE,
    Some(InstructionReportErrorCode.INVALID_BET_ID),
    UpdateInstruction(betId, newPersistenceType)
  )

  // Helper function to generate successful cancel report
  def genSuccessUpdateReport(betId: String, newPersistenceType: PersistenceType) = UpdateInstructionReport(
    InstructionReportStatus.SUCCESS,
    None,
    UpdateInstruction(betId, newPersistenceType)
  )

  val lapse = PersistenceType.LAPSE
  val persist = PersistenceType.PERSIST
  val marketOnClose =PersistenceType.MARKET_ON_CLOSE
}

