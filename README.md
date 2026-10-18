# Kevin Bay | Computer Science & Infrastructure Portfolio

Computer Science student at Colorado State University specializing in Network and Security.

---

## Systems, Networking & Infrastructure

### Embedded Hardware Penetration Testing & Bootloader Exploitation
- Performed a hardware-level security audit and privilege escalation attack on an ASUS RT-AX1800S/RT-AX54 wireless router powered by a MediaTek MT7621AT SoC (MIPS 1004Kc).

- **Physical Interface Tapping:** Located and probed an unpopulated debug header (`J1`) on the PCB using a digital multimeter; configured a Flipper Zero as a 3.3V UART serial bridge at 115200 baud to intercept early-stage boot diagnostic output.

- **Bootloader Interception & Memory Manipulation:** Bypassed U-Boot 2018.09 autoboot timing gates, mapped NAND partitions (`mtdparts`), and manipulated Flattened Device Tree (FDT) structures staged in DRAM to inject `init=/bin/sh console=ttyS0,115200` into `fdt chosen`, successfully hijacking kernel execution flow into an unauthenticated PID 1 root shell.

- **Firmware Cryptanalysis & Storage Access:** Audited MIPS-compiled shared binaries (`libshared.so`) to assess AES-128-ECB obfuscation and MD5 key derivation weaknesses; directly dumped stage-1 bootloader data from raw NAND flash blocks (`/dev/mtd0`) and verified arbitrary write execution in volatile kernel memory (`devtmpfs`).


### Proxmox Home Lab & Virtualization Environment
- Designed and deployed a dedicated bare-metal hypervisor environment to manage virtualized enterprise infrastructure.

- **Core Deployments:** Maintain active virtual machines for Kali Linux (isolated security testing) and automated application/Minecraft servers.

- **Hardware Allocation:** Built and optimized host hardware utilizing 200GB RAM allocations and an Nvidia Tesla P4 GPU for hardware-accelerated workloads.


### Enterprise IT Support | Warner College of Natural Resources (CSU)
- Serve as an IT Technician, diagnosing, formatting, and assembling enterprise hardware deployments for on-campus and remote faculty/staff.

- Manage multi-platform systems administration pipelines across diverse Linux and Windows operating systems.


---

## Software & Web Development

### Webmaster & Developer | Kappa Kappa Psi
- Designed, developed, and maintained the official website for the chapter organization, managing the full deployment lifecycle.

- Focused on clean UI/UX design, reliable hosting infrastructure, and accessible information architecture for members and alumni.


### Object-Oriented Java Applications (CSU CS214)
- Developed a robust series of programmatic solutions focusing on core data structures, advanced memory management, and clean object-oriented design principles.

- Gained extensive experience utilizing professional debugging, refactoring, and automated testing frameworks (JUnit).


### Low-Level Systems Programming in C++ (CSU CT301)
- Developed performance-focused applications utilizing C++ to understand low-level system interactions, runtime execution mechanics, and strict execution efficiency.

- **Core Mechanisms:** Gained extensive hands-on experience with manual memory management, explicit pointer manipulation, dynamic memory allocation (`new`/`delete`), and avoiding memory leaks.

- **System Engineering & Compilation:** Designed modular codebases across clean header (`.h`) and implementation (`.cpp`) structures, managing automated compilation pipelines via custom **Makefiles** and the **GCC/G++** compiler toolchain.

---

## Engineering & Automation Hobbies

### 3D Design, Printing & Hardware Customization
- Operate and maintain high-volume 3D printing equipment, including a heavily modified Sovol SV06 Plus.

- Deployed and configured a Klipper firmware environment to optimize kinematics, enable macro-driven automation, and unlock granular micro-stepping control for superior surface finish quality.

- Engineered custom hardware upgrades, including designing and wiring a custom 110mm cooling fan array to maximize print stability and thermal performance.
