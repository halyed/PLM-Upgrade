terraform {
  required_version = ">= 1.7"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  # Remote state — use S3 backend in production
  # backend "s3" {
  #   bucket = "plm-terraform-state"
  #   key    = "infra/terraform.tfstate"
  #   region = "eu-west-1"
  # }
}

provider "aws" {
  region = var.aws_region
}

module "vpc" {
  source = "./modules/vpc"

  project     = var.project
  environment = var.environment
  vpc_cidr    = var.vpc_cidr
}

module "eks" {
  source = "./modules/eks"

  project          = var.project
  environment      = var.environment
  vpc_id           = module.vpc.vpc_id
  private_subnets  = module.vpc.private_subnet_ids
  cluster_version  = var.eks_cluster_version
  node_instance_type = var.node_instance_type
  node_min_size    = var.node_min_size
  node_max_size    = var.node_max_size
  node_desired_size = var.node_desired_size
}

module "rds" {
  source = "./modules/rds"

  project          = var.project
  environment      = var.environment
  vpc_id           = module.vpc.vpc_id
  private_subnets  = module.vpc.private_subnet_ids
  db_name          = "plm"
  db_username      = var.db_username
  db_password      = var.db_password
  instance_class   = var.rds_instance_class
}

module "s3" {
  source = "./modules/s3"

  project     = var.project
  environment = var.environment
}
