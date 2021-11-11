# legal-i Agent

## Stresstest

```
make pull-pdfs

# Set property
legali.example.files-path=./pdfs
legali.example.runs=10
spring.task.execution.pool.max-size=5
spring.task.execution.pool.core-size=5
legali.default-metadata.legali.pipeline.disabled=true
```