FROM java:8
COPY *.jar /app.jar
CMD ["--server.port=7080"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
#使用镜像：
#docker build -t myimages ./
#docker images