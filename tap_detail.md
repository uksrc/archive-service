## Details of the TAP Service
Any information regarding the current TAP service in use.

### Running locally on minikube
1. Start minikube
2. Make sure the images created are added to minikube's docker repository and not to the host machine:
    ``` 
    eval $(minikube docker-env)
    Or on windows 
    & minikube -p minikube docker-env | Invoke-Expression``
    ```
3. check the tap.properties template for suitable values. See [*TAP Properties*](#tap-properties)
4. clean and build the archive-service
    ```
    gradle clean
    gradle build -x test
    ```
5. Expose the archive service on the minikube cluster and the displayed localhost:<port> should resolve the service (just add /tap)
    ```
    minikube service archive-service --url
    ```
   


