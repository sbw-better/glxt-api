FROM 10.23.114.51:3000/springcloud/java8:latest

MAINTAINER wuxingkun@citics.com

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD target/*.jar glxt-api.jar

EXPOSE 15009

ENTRYPOINT ["java", "-jar", "/glxt-api.jar"]
#ENTRYPOINT java ${JAVA_OPTS} -jar /glxt-api.jar
