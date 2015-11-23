package domain

import domain.InstructionReportErrorCode.InstructionReportErrorCode
import domain.InstructionReportStatus.InstructionReportStatus

/**
 * Created by Alcock on 25/10/2015.
 */

abstract class InstructionReport(val status: InstructionReportStatus, val errorCode: Option[InstructionReportErrorCode])