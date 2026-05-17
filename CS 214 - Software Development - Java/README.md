# Multi-Mode Music Recommendation & Predictive Analytics System

A high-performance Java application built to process large-scale user rating datasets, perform statistical analysis, execute data normalization, and deliver personalized media recommendations through custom algorithmic processing models. 

This system acts as a complete data analytics pipeline, translating raw, three-field CSV transaction inputs (`song, user, rating`) into actionable predictive models.

---

## Key Architectural Features & Processing Modes

The application relies on a flexible CLI argument-parsing dispatch mechanism that executes different algorithmic pipelines based on runtime execution flags:

### 1. Descriptive Statistical Analysis (Default Mode)
* Generates comprehensive metrics for entire datasets.
* Computes the exact density of sample sizes, dataset mean values, and statistical variance/standard deviation per asset to flag anomaly trends or highly polarized assets.

### 2. User Matrix Desparsification (`-a`)
* Identifies and filters out "uncooperative users" (accounts providing stagnant or zero-variance feedback).
* Normalizes multi-user data metrics and structures sparse multidimensional datasets into dense analytical matrices, mapping complex relational maps with standard `NaN` handling for unobserved vectors.

### 3. Collaborative Vector Similarity Filtering (`-u`)
* Calculates the geometric distance between individual media assets across multi-user evaluation planes.
* Evaluates relative mathematical distance using multidimensional Euclidean metrics across matching user data slices to generate deterministic structural similarity tables.

### 4. User Preference Prediction Engine (`-p`)
* Solves data sparsity constraints by calculating unobserved user ratings.
* Leverages a multi-tiered predictive logic system: dynamically identifies the most similar neighbor profile to project expected trends using personal variance baselines (`mean` and `stdDev`). If neighbor baselines are absent, it falls back to a custom user/item sample-weighted distribution model.

### 5. Multi-Centroid $K$-Means Clustering & Recommendation (`-r`)
* Implements a bare-metal execution of the **$K$-Means Clustering Algorithm** to dynamically segment the complete library.
* Initializes user-selected tracks as multi-dimensional coordinate centroids, normalizes vector states across the entire user base, and optimizes data clustering profiles over progressive mathematical iterations to output refined target recommendations.

### 6. Automated Playlist Generation (`-s`)
* Integrates directly with downstream playlist synthesis engines, passing programmatic arrays, strict size limitations ($K$), constraint validations, and filtered array lists.

---

## Software Engineering & Technical Concepts Demonstrated

* **Data Normalization & Mathematical Modeling:** Extensive implementation of statistical transformations, converting raw ordinal boundaries (1–5 ratings) into standardized $Z$-scores to eliminate individual user rating bias:
  $$Z = \frac{x - \mu}{\sigma}$$
* **Data Structures & Memory Efficiency:** Utilizes optimized Java Collections Framework variants (`HashMap`, `TreeSet`, nested mapping structures) to perform low-overhead multidimensional data lookups.
* **Robust Stream Processing:** Leverages functional Java Streams, lambda predicates, and reduction pipelines to map, group, filter, and calculate massive primitive arrays elegantly.
* **Defensive Programming & Verification:** Features comprehensive runtime constraint verification, custom exception handling for structural file processing, data boundary protection, and strict automated testing paradigms (JUnit).

---

## System Architecture & I/O Specifications

### Input Data Format
Accepts clean, header
