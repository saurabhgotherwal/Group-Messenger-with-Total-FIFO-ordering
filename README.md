# Group-Messenger-with-Total-FIFO-ordering
This is an implementation of a group messenger that can send message to multiple devices that guarantees Total and FIFO ordering and store them in a permanent key-value storage. 

This application is a very good example to demonstrate how distributed system works in conjunction with each other. 

This is an Android fault tolerant group chat messenger that multicasts a message to all app instances and provides both TOTAL and FIFO ordering guarantees. The messages are stored in a permanent key-value storage.
To achieve the ordering, I have implemented a modified version of ISIS algorithm that provides decentralization and consistency even under application failures.

Reference for message ordering. (http://www.cs.uic.edu/~ajayk/Chapter6.pdf)
