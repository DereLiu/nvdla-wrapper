// See LICENSE for license details.
package nvidia.blocks.dla

import chisel3._
import freechips.rocketchip.config.Field
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, BufferParams, AddressSet, Description, Device, DeviceSnippet, Resource, ResourceBinding, ResourceInt, ResourceReference, ResourceBindings, ResourceAddress, ResourcePermissions, ResourceAnchors, SimpleDevice}
import freechips.rocketchip.tilelink.{TLBuffer, TLIdentityNode}

case object NVDLAKey extends Field[Option[NVDLAParams]](None)
case object NVDLAFrontBusExtraBuffers extends Field[Int](0)
case class NVDLAReservedMemParams(base: BigInt, size: BigInt)
case object NVDLAReservedMemKey extends Field[Option[NVDLAReservedMemParams]](None)

trait CanHavePeripheryNVDLA { this: BaseSubsystem =>
  p(NVDLAKey).map { params =>
    val nvdla = LazyModule(new NVDLA(params))
    //connect with fbus
    fbus.fromMaster(name = Some("nvdla_dbb"), buffer = BufferParams.default) {
      TLBuffer.chainNode(p(NVDLAFrontBusExtraBuffers))
    } := nvdla.dbb_tl_node

    pbus.toFixedWidthSingleBeatSlave(4, Some("nvdla_cfg")) { nvdla.cfg_tl_node }

    ibus.fromSync := nvdla.int_node

    val nvdlaResvRoot = p(NVDLAReservedMemKey).map { _ =>
      new DeviceSnippet {
        override def parent = None
        def describe(): Description = Description("reserved-memory", Map(
          "#address-cells" -> Seq(ResourceInt(2)),
          "#size-cells"    -> Seq(ResourceInt(2)),
          "ranges"         -> Nil))
      }
    }

    p(NVDLAReservedMemKey).foreach { resv =>
      val region = new SimpleDevice("nvdla_reserved", Seq("shared-dma-pool")) {
        override def parent = Some(nvdlaResvRoot.getOrElse(super.parent.get))
        override def describe(resources: ResourceBindings): Description = {
          val Description(name, mapping) = super.describe(resources)
          Description(name, mapping ++ Map("no-map" -> Nil))
        }
      }
      val addrSets = AddressSet.misaligned(resv.base, resv.size)
      val perms = ResourcePermissions(r = true, w = true, x = false, c = false, a = false)
      ResourceBinding {
        Resource(region, "reg").bind(ResourceAddress(addrSets, perms))
        Resource(nvdla.dtsdevice, "memory-region").bind(ResourceReference(region.label))
      }
    }
  }
}
