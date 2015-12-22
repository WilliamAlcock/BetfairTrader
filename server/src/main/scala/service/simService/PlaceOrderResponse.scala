package service.simService

import domain.PlaceInstructionReport

case class PlaceOrderResponse[T](result: T, report: PlaceInstructionReport)

