FROM python:3.8.5-slim

WORKDIR /tests

COPY ./entrypoint.sh /

RUN apt-get update \
  && apt-get install \
  curl \
  unzip \
  jq -y && \
  apt-get clean

ARG QUICK_CLI_VERSION 0.4.0
ENV BATS_VERSION 1.6.0

RUN curl -#L https://github.com/bats-core/bats-core/archive/v${BATS_VERSION}.zip -o bats.zip \
    && unzip ./bats.zip \
    && bash bats-core-${BATS_VERSION}/install.sh /usr/local \
    && rm -rf ./bats-core-${BATS_VERSION} \
    && rm -rf ./bats.zip \
    && pip install aiohttp==3.8.1 \
    && pip install gql==3.0.0 \
    && pip install quick-cli==${QUICK_CLI_VERSION} \
    && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
