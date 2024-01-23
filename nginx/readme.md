# Run
From the project root
```bash 
docker build -t test -f nginx/Dockerfile . && docker run --rm -p 8080:80 test
```
