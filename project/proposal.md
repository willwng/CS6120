**What will you do?**

We will work on [AIRduct](https://squera.github.io/fcs-pdf/dingFCS2023.pdf), the array-based IR for the [Viaduct](https://www.cs.cornell.edu/andru/papers/viaduct/viaduct-tr.pdf) compiler.
This is based on a research project that Vivian has been working on for the last couple of years.

Viaduct is a compiler which synthesizes secure, distributed programs employing an extensible suite of cryptography.
It uses security specifications in the form of information flow labels to select cryptographic protocols that make the
compiled program secure. In this project, we'll assume this choice has been made correctly, and focus on the IR.

AIRduct is unique in that it is an IR for a cryptographic compiler which is capable of representing programs which
cannot be implemented as a single crytographic circuit. This enables support for interactive programs, and programs
mixing multiple cryptographic back ends and local computation, which is useful for performance.
AIRduct represents code employing multiple cryptographic mechanisms as structured control flow over calls to circuit
functions. This allows for a separation of impure behavior (I/O) and control flow from pure cryptographic computation,
which does not allow complex control flow.
We use an array-based IR since this preserves useful structures upon which we can optimize computations.

Cryptographic back-ends (extensible portions of the compiler which implement crypto mechanisms) can (probably) optimize
most aggressively when the circuit functions are larger. So, one aspect of implementing the IR we are particularly
interested in is how to produce maximally large circuit blocks.

We are focusing on implementing the following:
- Supporting control flow (if, while)
- A grouping procedure for creating circuit blocks (in the IR) from programs in which each statement is annotated with
a protocol. This will include reordering of code.
- Optimizations by back-ends, to show the fruits of our work which enables them

**How will you do it?**

- Control flow should hopefully be straightforward.
- Grouping requires analysis regarding which reorderings are safe, with respect to both data dependencies and security.
- Optimizations will be non-trivial but inspired by prior work.
    - One optimization is simply calling into SIMD instructions which are natively supported by many cryptographic
    libraries.
    - A more interesting optimization is the combination of bulk instructions. One example is merging two array map 
    expressions into one, when the second map operates on the output of the first one (similar to loop fusion).

**How will you empirically measure success?**

- We will measure success of implementing control flow checking correctness on existing test cases.
- We are not confident how to measure success of grouping yet. A good metric for the quality of a grouping might be the
resulting number of blocks, but it's not obvious whether one can compute the minimal number of blocks to compare our
implementation to. There doesn't exist a unique maximal grouping, but there definitely exists some set of maximal
groupings for any given program.
- The input code will be manually written test cases. We will try to write programs which use each implemented
feature, such as taking different paths through the CFG and taking advantage of vectorization. We will also include
benchmark programs, which were used by the original Viaduct paper and are also often used to evaluate other
cryptographic compilers.
- We will test for correctness by ensuring that output is the same with and without calling into cryptographic
protocols, that is, a computation using cryptography should yield the same result as in cleartext; the only difference
being the added security properties.
- We will think very hard about security. This is something that needs to be reasoned about abstractly rather than 
relying on testing.
- We can measure the benefit of optimizations by examining run time.

**Team members:**
[Vivian Ding](https://github.com/vivianyyd)
[William Wang](https://github.com/willwng)
