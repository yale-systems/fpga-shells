package sifive.fpgashells.shell.xilinx

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class QSFPXilinxPlacedOverlay(name: String, di: QSFPDesignInput, si: QSFPShellInput)
  extends QSFPPlacedOverlay(name, di, si)
{
  def shell: XilinxShell

  shell { InModuleBody {
    UIntToAnalog(qsfpSink.bundle.tx_p, io.tx_p, true.B)
    UIntToAnalog(qsfpSink.bundle.tx_n, io.tx_n, true.B)
    qsfpSink.bundle.rx_p := AnalogToUInt(io.rx_p)
    qsfpSink.bundle.rx_n := AnalogToUInt(io.rx_n)
    qsfpSink.bundle.refclk_p := AnalogToUInt(io.refclk_p)
    qsfpSink.bundle.refclk_n := AnalogToUInt(io.refclk_n)
    UIntToAnalog(qsfpSink.bundle.modsell, io.modsell, true.B)
    UIntToAnalog(qsfpSink.bundle.resetl, io.resetl, true.B)
    qsfpSink.bundle.modprsl := AnalogToUInt(io.modprsl)
    qsfpSink.bundle.intl := AnalogToUInt(io.intl)
    UIntToAnalog(qsfpSink.bundle.lpmode, io.lpmode, true.B)
  } }
}
