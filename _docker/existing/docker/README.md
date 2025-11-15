# Docker Compose Environment Setup

## Changes Made

The volume mappings in the `docker-compose-env.yml` file have been updated to use the `/mydata/` prefix for all containers. For example:
- `./minio/data` has been changed to `/mydata/minio/data`
- `./minio/config` has been changed to `/mydata/minio/config`

This pattern has been applied to all containers in the docker-compose file.

## Directory Structure

A script has been provided to create all the necessary directories:

```
/mydata/
├── mysql/
│   ├── conf/
│   └── data/
├── sql/
│   └── mysql/
├── nacos/
│   └── logs/
├── seata/
│   ├── config/
│   └── logs/
├── redis/
│   ├── data/
│   └── config/
├── minio/
│   ├── data/
│   └── config/
├── xxljob/
│   └── logs/
└── nginx/
    ├── conf/
    │   └── conf.d/
    ├── html/
    └── logs/
```

## Usage

1. Run the `create_directories.sh` script to create all the necessary directories:

```bash
sudo ./create_nginx_env.sh
```

2. Create a `.env` file based on the `.env.example` file:

```bash
cp .env.example .env
# Edit the .env file to set your passwords and credentials
```

3. Start the infrastructure containers using the `docker-compose-env.yml` file:

```bash
docker-compose -f docker-compose-env.yml up -d
```

This will start all the infrastructure containers (MySQL, Redis, Minio, etc.) with the updated volume mappings.

4. Start the application container using the `docker-compose-app.yml` file:

```bash
docker-compose -f docker-compose-app.yml up -d
```

This will start the retail-api application container, which will connect to the infrastructure containers.

## Troubleshooting

### Network Error

If you encounter the following error when running `docker-compose-app.yml`:

```
ERROR: Network youlai-boot declared as external, but could not be found. Please create the network manually using `docker network create youlai-boot` and try again.
```

This means that the Docker network "youlai-boot" doesn't exist yet. You have two options:

1. Run `docker-compose-env.yml` first, which will create the network:

```bash
docker-compose -f docker-compose-env.yml up -d
```

2. Or create the network manually:

```bash
docker network create youlai-boot
```

Then try running `docker-compose-app.yml` again.
