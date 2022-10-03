## How does JQF run with it's own JUnit runner and where is the fuzzing loop?
`RunWith(JQF.class)` makes sure that JUnit will use runner in `JQF.java`, and in `JQF.java`, the override `methodBlock` method returns `FuzzStatement`, where
`evaluate()` method is override and provides test result check and fuzzing loop.
