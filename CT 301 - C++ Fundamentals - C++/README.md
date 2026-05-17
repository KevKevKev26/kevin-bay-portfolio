# Cellular Automata Engine & Simulation Shell (Conway's Game of Life)

A high-performance C++ implementation of Conway's Game of Life simulation, featuring customizable cell-state variations, cross-platform argument parsing layers, and an interactive inline diagnostic shell supporting live memory grid manipulation and transactional generation rollback mechanics.

---

## Core Engineering Achievements

### Interactive Diagnostic Shell & Generation Rollback
* Engineered a manual execution intercept layer (`on_pause`) that halts simulation loops at specified evaluation intervals, spinning up an interactive user shell pipeline.
* Developed an array-backed state history ring buffer that allows users to perform generation rollbacks (`r [count]`), reverting system states backward through the cache pipeline.
* Supports real-time memory adjustments via runtime stream commands, allowing direct runtime injection of live (`a`), dead (`d`), or decaying (`e`) cellular states at specific coordinate bounds.

### Cross-Platform Compilation & Conditional Preprocessing
* Solved environmental differences between POSIX/Unix systems and Windows native environments.
* Utilizes conditional preprocessor directives (`#ifndef _WIN32`) to implement a robust Unix native POSIX standard `getopt` CLI parser while cleanly falling back to a custom-engineered linear argument tokenizer on Windows environments.

### Modular Architecture & Custom State Extensions
* Decoupled system components into strict object-oriented layers: input deserialization (`FileParser`), core logic simulation matrices (`GameOfLife`), and the primary execution controller (`main`).
* Extended the classic 2-state binary logic matrix into an advanced three-state model featuring a custom cellular decay paradigm (`decay_char`) alongside optional coordinate wrap-around topology rules.

---

## Systems Concepts Demonstrated

* **Manual Memory Grid Tracking:** High-efficiency multi-dimensional mapping using raw arrays and vectorized states to track neighbor thresholds.
* **Stream Deserialization & Validation:** Implemented strict input file validation with granular bounds checking, string tokenization, and comprehensive format-exception handling.
* **Low-Level Native Commands:** Leveraged native POSIX execution integrations (`mkdir -p`) wrapped directly inside native C runtime system calls for dynamic output file directory allocation.

---

## Compilation & Usage Directions

### Project Compilation
Build the application binary natively utilizing the custom `Makefile` via GCC/G++:
```bash
make
