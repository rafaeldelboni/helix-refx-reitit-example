FROM nginx
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY public /usr/share/nginx/html
