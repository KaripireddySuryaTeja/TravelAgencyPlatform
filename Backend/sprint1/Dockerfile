FROM public.ecr.aws/docker/library/openjdk:22-ea-17-slim-bookworm
COPY ./target/*.jar /home/app.jar
CMD ["java","-jar","/home/app.jar"]
