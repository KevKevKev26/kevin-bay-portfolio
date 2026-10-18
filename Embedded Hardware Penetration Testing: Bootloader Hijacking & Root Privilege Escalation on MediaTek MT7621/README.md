# Embedded Hardware Penetration Testing: Bootloader Hijacking & Privilege Escalation on MediaTek MT7621

An end-to-end hardware-level penetration test and firmware security audit targeting an ASUS RT-AX1800S/RT-AX54 wireless router powered by the MediaTek MT7621AT SoC[cite: 4]. The assessment evaluated physical debug port exposure, bootloader integrity mechanisms, runtime device-tree tampering in volatile memory, and local configuration security[cite: 2, 4, 5, 6].

---

## 1. System Specifications & Assessment Setup

| Component | Technical Specification |
| :--- | :--- |
| **Target Device** | ASUS RT-AX1800S / RT-AX54 (Board Revision 1.0.0.1)[cite: 4] |
| **Processing Architecture** | MediaTek MT7621AT (MIPS 1004Kc, Dual-Core / 4 Threads @ 880 MHz)[cite: 4] |
| **Volatile / Non-Volatile Storage** | 256 MiB DDR3 RAM / 128 MiB SPI NAND Flash (NMBM translation layer)[cite: 4] |
| **Operating System** | ASUSWRT (Linux Kernel 4.4.198 MIPS, musl libc) |
| **Physical Interface Intercepted** | Unpopulated J1 Debug Header (3.3V TTL UART @ 115200 baud)[cite: 2, 4, 5] |
| **Hardware Tools** | Flipper Zero (Serial/UART bridge), Digital Multimeter[cite: 2, 3] |
| **Host Environment** | Linux / picocom serial terminal (`/dev/ttyACM0`)[cite: 1] |

---

## 2. Threat Model & Exploit Architecture

The threat model assumes an attacker with brief physical access seeking to bypass operating system authentication, escalate to an unrestricted root execution environment, and audit firmware confidentiality without leaving persistent software indicators[cite: 2, 6].

```
+-------------------------------------------------------------+
|              1. Physical Interface Isolation                |
|  - Traced unpopulated J1 header pins with multimeter        |
|  - Verified GND continuity & 3.3V logic levels              |
|  - Bridged TX/RX to Flipper Zero UART GPIO (Pins 13/14)     |
+------------------------------+------------------------------+
                               |
                               v
+------------------------------+------------------------------+
|               2. Bootloader Interception                    |
|  - Timed keyboard break sequence on U-Boot 2018.09          |
|  - Halted autoboot window (bootdelay=0 override)            |
|  - Reconstructed NAND partition boundaries via mtdparts     |
+------------------------------+------------------------------+
                               |
                               v
+------------------------------+------------------------------+
|            3. In-Memory Kernel Execution Hijack             |
|  - Staged FIT container from flash offset 0x3e0000 to RAM   |
|  - Isolated Flattened Device Tree (FDT) blob to 0x83000000  |
|  - Injected runtime directives (init=/bin/sh) via fdt chosen|
|  - Triggered sub-image execution via bootm                  |
+------------------------------+------------------------------+
                               |
                               v
+------------------------------+------------------------------+
|             4. Hardware Control & Storage Audit             |
|  - Gained unrestricted PID 1 root execution (UID 0)         |
|  - Dumped raw SPI NAND flash blocks via /dev/mtd*           |
|  - Proved memory-space arbitrary write capability           |
+-------------------------------------------------------------+
```

---

## 3. Technical Methodology & Execution

### Phase 1: Physical Interface Identification & Serial Bridging
* Located an unpopulated 4-pin debug header (`J1`) on the router's mainboard[cite: 2].
* Used a digital multimeter to measure continuity against the RJ45 shielding ground cage, identifying `Pin 1 (3.3V VCC)`, `Pin 2 (RX)`, `Pin 3 (TX)`, and `Pin 4 (GND)`[cite: 3].
* Left the 3.3V VCC pin floating to prevent bus contention with the host workstation[cite: 2].
* Connected flying leads directly to a Flipper Zero configured as an external USB-to-UART bridge running at 115200 baud, 8 data bits, no parity, and 1 stop bit (8N1)[cite: 1, 2, 5].

### Phase 2: Bootloader Interception & Memory Map Reconstruction
* During power-on reset, MediaTek's `U-Boot 2018.09` initialized the NAND Management Byte Model (NMBM)[cite: 4].
* Interrupted the boot sequence by sending timed break characters during the autoboot countdown window, landing in the interactive U-Boot CLI (`=>`)[cite: 4].
* Queried system memory configuration (`bdinfo`) and non-volatile NAND partitions (`mtdparts`)[cite: 5]:
  * `0x00000000 - 0x000e0000`: `u-boot` (Stage-1 bootloader)[cite: 5]
  * `0x000e0000 - 0x001e0000`: `u-boot-env` (Boot environment variables)[cite: 5]
  * `0x001e0000 - 0x003e0000`: `factory` (EEPROM calibration and MAC data)[cite: 5]
  * `0x003e0000 - 0x07800000`: `firmware` (FIT container holding Kernel and RootFS)[cite: 5]

### Phase 3: Flattened Device Tree Modification & Boot Redirection
* The router's firmware is packaged inside a Flattened Image Tree (FIT) container with internal cryptographic hashing. Modifying kernel command-line variables in the U-Boot environment corrupted image verification checks.
* **Staging to RAM:** Copied the kernel and device tree structure from raw NAND storage into DRAM at `0x82000000`.
* **DTB Relocation:** Relocated the 11 KB device tree blob to staging RAM at `0x83000000` to prevent structural overlap.
* **Argument Injection:** Injected custom boot directives into the `chosen` device tree node:
  ```text
  console=ttyS0,115200 init=/bin/sh rootfstype=squashfs,jffs2
  ```
* **Direct Execution:** Executed the staged kernel using sub-image offsets (`bootm 0x82000000:kernel@1 - 0x83000000`), instructing the kernel to spawn directly into a single-user root shell, completely bypassing the vendor's user-space initialization scripts (`/sbin/init` and `/sbin/rc`)[cite: 6].

### Phase 4: Firmware Cryptanalysis & Storage Access
* Audited the cryptographic implementations inside compiled MIPS binaries (`libshared.so` and `libpasswd.so`).
* Discovered that administrative credentials in non-volatile storage (`http_passwd`) are encrypted using tiny-AES in Electronic Codebook mode (`AES-128-ECB`) with an MD5-derived key.
* Verified that when the dynamic salt generation routine (`make_salt`) fails or is absent from NVRAM, fallback routines complicate plaintext reconstruction, but direct hardware manipulation remains completely unrestricted.

---

## 4. Verification & Proof of Compromise

The following terminal captures demonstrate proof of arbitrary execution, hardware memory mapping, and raw storage extraction[cite: 6]:

```
/ # dmesg | grep -i "Kernel command line"
[    0.000000] Kernel command line: console=ttyS0,115200 init=/bin/sh rootfstype=squashfs,jffs2

/ # hexdump -C -n 64 /dev/mtd0
00000000  27 05 19 56 12 d8 05 59  00 00 00 00 00 01 b0 c4  |'..V...Y........|
00000010  80 10 00 00 80 10 00 00  ba 02 f7 aa 11 05 01 00  |................|
00000020  4d 54 37 36 32 31 20 4e  41 4e 44 00 00 00 00 00  |MT7621 NAND.....|
00000030  00 00 00 40 00 00 00 00  00 00 00 00 4f a5 9e 42  |...@........O..B|
00000040

/ # echo "ROOT-EXECUTION" > /dev/proof.txt && cat /dev/proof.txt && rm /dev/proof.txt
ROOT-EXECUTION
```

* **Kernel Command-Line Hijack:** Output from `dmesg` confirms execution flowed directly through the custom `init=/bin/sh` parameter injected via the modified Device Tree Blob in memory[cite: 6].
* **Raw Flash Storage Access:** The hexdump of `/dev/mtd0` displays the standard U-Boot header magic bytes (`27 05 19 56`) and the stage-1 SoC partition header (`MT7621 NAND`), proving unprivileged read access to raw non-volatile media[cite: 6].
* **Arbitrary Memory Write Authority:** Successfully created, read, and deleted data directly within kernel device memory (`devtmpfs`), proving full read/write privilege on the host system[cite: 6].

---

## 5. Security Vulnerabilities & Remediation

| Vulnerability | Severity | Impact | Mitigation Strategy |
| :--- | :--- | :--- | :--- |
| **Exposed Serial Console (UART)** | Medium | Grants immediate bus-level access to early-stage boot diagnostic output and interactive prompts[cite: 1, 4, 5]. | Disable UART RX/TX pins in SoC pinmux configurations for production revisions; physically remove or un-route test pads[cite: 2]. |
| **Insecure Autoboot Configuration** | High | U-Boot listens for serial break characters during power-on reset, permitting arbitrary execution flow interception[cite: 4]. | Set `bootdelay=-2` to disable keyboard polling completely; enforce bootloader password verification on console access. |
| **Weak Credential Obfuscation** | Medium | AES-128 in ECB mode lacks diffusion across blocks; MD5-based key generation provides inadequate cryptographic resistance. | Store credentials using standard salted, compute-heavy key derivation functions (e.g., Argon2id or PBKDF2); offload private secrets to hardware security elements. |

---

## 6. Visual Documentation Guide

* **Figure 1 (Hardware Setup):** Workbench layout showing the opened router chassis, multimeter probes validating board ground, and the Flipper Zero connected via USB to the laptop terminal running `picocom`[cite: 1, 2, 3].
* **Figure 2 (Wiring Close-Up):** Macro detail of the soldered/tapped flying leads on header `J1` routed cleanly into the Flipper Zero GPIO breakout headers[cite: 2, 3].
* **Figure 3 (Bootloader Interception):** Serial console screenshot capturing the MediaTek MT7621 boot banner and the interactive U-Boot command prompt (`=>`)[cite: 4].
* **Figure 4 (Memory & Storage Analysis):** Console output showing memory base pointers (`bdinfo`) and storage boundary mapping (`mtdparts`)[cite: 5].
* **Figure 5 (Root Shell Compromise):** Terminal capture showing kernel boot argument verification, the MTD0 raw bootloader header dump, and arbitrary memory write validation[cite: 6].

---

## 7. Summary

* **Embedded Hardware Security & Interface Extraction:** Located, probed, and tapped unpopulated board-level UART interfaces on MediaTek MT7621 network hardware using a digital multimeter and hardware protocol bridge, establishing a live serial console connection at 115200 baud[cite: 1, 2, 3, 4].
* **Bootloader Interception & Memory Manipulation:** Bypassed U-Boot autoboot timing controls, mapped raw NAND partitions, and dynamically modified RAM-staged Flattened Device Trees (FDT) to inject `init=/bin/sh`, redirecting kernel execution to bypass user authentication[cite: 4, 5, 6].
* **Firmware Cryptanalysis & Low-Level Storage Access:** Audited MIPS-compiled shared binaries (`libshared.so`) to assess AES-128-ECB and MD5 key derivation routines; extracted raw bootloader blocks directly from physical flash (`/dev/mtd0`) via root execution[cite: 6].
