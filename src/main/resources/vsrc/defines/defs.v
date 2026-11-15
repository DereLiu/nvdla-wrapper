`define FPGA
`define SYNTHESIS
`define DESIGNWARE_NOEXIST
`define VLIB_BYPASS_POWER_CG
`define NV_FPGA_SYSTEM
`define NV_FPGA_FIFOGEN
`define NV_FPGA_UNIT
// riscv_iommu.preprocessed.sv (lowRISC prim_assert) defines a COVER(...)
// macro. Vivado reads all sources as one compilation unit, so when it parses
// that file first the macro reaches the "ifdef COVER" testpoint blocks in
// NVDLA, which instantiate the simulator-only system_verilog_testpoint.
`ifdef COVER
`undef COVER
`endif
