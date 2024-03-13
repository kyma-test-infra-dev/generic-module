FROM sapmachine:17.0.8 AS build

RUN apt-get -y update \
&& apt-get -y install maven

WORKDIR /operator
COPY . .
RUN mvn clean package

FROM sapmachine:17.0.8

COPY release/image/file-system/ /

COPY --from=build /operator/connectivity-proxy-operator/target/connectivity-proxy-operator-1.0.0-SNAPSHOT-assembly.zip /usr/local/share/connectivity-proxy-operator/connectivity-proxy-operator.zip

RUN apt-get -y update \
&& apt-get -y install unzip \
&& unzip "/usr/local/share/connectivity-proxy-operator/connectivity-proxy-operator.zip" -d "/usr/local/share/connectivity-proxy-operator" \
&& find /usr/local/bin/ -type f -exec chmod +x {} \;

ENV CONNECTIVITY_PROXY_SCRIPTS_DIR=/usr/local/bin JAVA_HOME=/usr/lib/jvm/sapmachine-17

ENTRYPOINT ["start-application.sh"]
