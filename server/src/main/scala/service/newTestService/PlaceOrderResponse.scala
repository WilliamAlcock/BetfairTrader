package service.newTestService

import domain.PlaceInstructionReport

case class PlaceOrderResponse[T](result: T, report: PlaceInstructionReport)

