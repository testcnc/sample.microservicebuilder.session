FROM websphere-liberty:microProfile
RUN installUtility install --acceptLicense logstashCollector-1.0
COPY server.xml /config/server.xml
RUN installUtility install --acceptLicense defaultServer
COPY target/microservice-session-1.0.0-SNAPSHOT.war /config/apps/session.war
