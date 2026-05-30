Graph

- support adjacent, succ, pred, degrees on nodes
- support add, remove nodes, edges
- auxiliary table for node -> in, out, adjacent, deg, ...

Node

- identified by integer id?

Control Flow Graph

- nodes are instructions
    - hold def, use, instr
- edges are possible control flow

Interference graph

- nodes are variables
- edges are interference relations
- obtained from flow graph

Liveness analysis

- live intervals or precise live ranges
