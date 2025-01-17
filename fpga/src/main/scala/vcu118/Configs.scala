package chipyard.fpga.vcu118

import sys.process._

import freechips.rocketchip.config.{Config, Parameters}
import freechips.rocketchip.subsystem.{SystemBusKey, PeripheryBusKey, ControlBusKey, ExtMem}
import freechips.rocketchip.devices.debug.{DebugModuleKey, ExportDebug, JTAG}
import freechips.rocketchip.devices.tilelink.{DevNullParams, BootROMLocated}
import freechips.rocketchip.diplomacy.{DTSModel, DTSTimebase, RegionType, AddressSet}
import freechips.rocketchip.tile.{XLen}

import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}

import sifive.fpgashells.shell.{DesignKey}
import sifive.fpgashells.shell.xilinx.{VCU118ShellPMOD, VCU118DDRSize}

import testchipip.{SerialTLKey}

import chipyard.{BuildSystem, ExtTLMem, DefaultClockFrequencyKey}

class WithDefaultPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(SPIParams(rAddress = BigInt(0x64001000L)))
  case VCU118ShellPMOD => "SDIO"
})

class WithSystemModifications extends Config((site, here, up) => {
  case DTSTimebase => BigInt((1e6).toLong)
  case BootROMLocated(x) => up(BootROMLocated(x), site).map { p =>
    // invoke makefile for sdboot
    val freqMHz = (site(DefaultClockFrequencyKey) * 1e6).toLong
    val make = s"make -C fpga/src/main/resources/vcu118/sdboot PBUS_CLK=${freqMHz} bin"
    require (make.! == 0, "Failed to build bootrom")
    p.copy(hang = 0x10000, contentFileName = s"./fpga/src/main/resources/vcu118/sdboot/build/sdboot.bin")
  }
  case ExtMem => up(ExtMem, site).map(x => x.copy(master = x.master.copy(size = site(VCU118DDRSize)))) // set extmem to DDR size
  case SerialTLKey => None // remove serialized tl port
})

// DOC include start: AbstractVCU118 and Rocket
class WithVCU118Tweaks extends Config(
  // harness binders
  new WithUART ++
  new WithSPISDCard ++
  new WithDDRMem ++
  // io binders
  new WithUARTIOPassthrough ++
  new WithSPIIOPassthrough ++
  new WithTLIOPassthrough ++
  // other configuration
  new WithDefaultPeripherals ++
  new chipyard.config.WithTLBackingMemory ++ // use TL backing memory
  new WithSystemModifications ++ // setup busses, use sdboot bootrom, setup ext. mem. size
  new chipyard.config.WithNoDebug ++ // remove debug module
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++
  new WithFPGAFrequency(100) // default 100MHz freq
)

/* ---------- DSAGen2 on VCU 118 Start ---------- */

class DevDSARocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.DevDSARocketConfig
)

class MeshDSARocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.MeshDSARocketConfig
)

class DualMeshDSARocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.DualMeshDSARocketConfig
)

class TriMeshDSARocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.TriMeshDSARocketConfig
)

class QuadMeshDSARocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.QuadMeshDSARocketConfig
)

class DSAGenVisionVCU118Config extends Config(
  new WithVCU118Tweaks ++
    new dsagen2.comp.impl.ip.WithDSAOverlay ++
    new chipyard.DSAGenVisionRocketConfig
)

// General Hard-write
class MeshDSARocketVCU118Config100MHz extends Config(new WithFPGAFreq100MHz ++ new MeshDSARocketVCU118Config)
class MeshDSARocketVCU118Config75MHz extends Config(new WithFPGAFreq75MHz ++ new MeshDSARocketVCU118Config)
class MeshDSARocketVCU118Config50MHz extends Config(new WithFPGAFreq50MHz ++ new MeshDSARocketVCU118Config)
class DualMeshDSARocketVCU118Config100MHz extends Config(new WithFPGAFreq100MHz ++ new DualMeshDSARocketVCU118Config)
class DualMeshDSARocketVCU118Config75MHz extends Config(new WithFPGAFreq75MHz ++ new DualMeshDSARocketVCU118Config)
class QuadMeshDSARocketVCU118Config100MHz extends Config(new WithFPGAFreq100MHz ++ new QuadMeshDSARocketVCU118Config)
class QuadMeshDSARocketVCU118Config75MHz extends Config(new WithFPGAFreq75MHz ++ new QuadMeshDSARocketVCU118Config)

// DSE Generated
class MeshDSAGenVisionVCU118Config100MHz extends Config(new WithFPGAFreq100MHz ++ new DSAGenVisionVCU118Config)

/* ---------- DSAGen2 on VCU 118 End ---------- */

class RocketVCU118Config extends Config(
  new WithVCU118Tweaks ++
  new chipyard.RocketConfig)
// DOC include end: AbstractVCU118 and Rocket

class BoomVCU118Config extends Config(
  new WithFPGAFrequency(50) ++
  new WithVCU118Tweaks ++
  new chipyard.MegaBoomConfig)

class WithFPGAFrequency(fMHz: Double) extends Config(
  new chipyard.config.WithPeripheryBusFrequency(fMHz) ++ // assumes using PBUS as default freq.
  new chipyard.config.WithMemoryBusFrequency(fMHz)
)

class WithFPGAFreq25MHz extends WithFPGAFrequency(25)
class WithFPGAFreq50MHz extends WithFPGAFrequency(50)
class WithFPGAFreq75MHz extends WithFPGAFrequency(75)
class WithFPGAFreq100MHz extends WithFPGAFrequency(100)
