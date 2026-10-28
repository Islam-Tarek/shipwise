FROM temporalio/temporal:1.8.3
USER root
RUN apk add --no-cache socat
COPY temporal-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
