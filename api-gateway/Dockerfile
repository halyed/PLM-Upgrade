FROM nginx:1.25-alpine

# Copy both configs
COPY nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY nginx/nginx-init.conf     /etc/nginx/nginx-init.conf

EXPOSE 80 443

# At startup: substitute ${DOMAIN} in the template, write nginx.conf, start nginx
CMD ["/bin/sh", "-c", \
  "envsubst '${DOMAIN}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && \
   nginx -g 'daemon off;'"]
