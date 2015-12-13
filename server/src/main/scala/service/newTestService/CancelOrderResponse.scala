package service.newTestService

import domain.CancelInstructionReport

case class CancelOrderResponse[T](result: T, report: CancelInstructionReport)

