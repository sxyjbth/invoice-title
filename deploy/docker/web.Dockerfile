FROM m.daocloud.io/docker.io/library/node:24.11.1-alpine3.22 AS builder

WORKDIR /workspace
RUN npm install --global pnpm@10.17.1

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY frontend/employee-h5/package.json frontend/employee-h5/package.json
COPY frontend/finance-admin/package.json frontend/finance-admin/package.json
RUN pnpm install --frozen-lockfile

COPY frontend/employee-h5 frontend/employee-h5
COPY frontend/finance-admin frontend/finance-admin

RUN VITE_PUBLIC_BASE=/invoice/employee/ \
    VITE_API_BASE_PREFIX=/invoice \
    pnpm --filter @invoice-title/employee-h5 build

RUN VITE_PUBLIC_BASE=/invoice/finance/ \
    VITE_API_BASE_PREFIX=/invoice \
    pnpm --filter @invoice-title/finance-admin build

FROM m.daocloud.io/docker.io/library/nginx:1.28.0-alpine

COPY deploy/docker/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /workspace/frontend/employee-h5/dist /usr/share/nginx/html/invoice/employee
COPY --from=builder /workspace/frontend/finance-admin/dist /usr/share/nginx/html/invoice/finance

RUN chown -R nginx:nginx /usr/share/nginx/html

USER nginx
EXPOSE 8080
ENTRYPOINT ["nginx"]
CMD ["-g", "daemon off;"]
