output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS API server endpoint"
  value       = module.eks.cluster_endpoint
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.rds.endpoint
  sensitive   = true
}

output "s3_cad_bucket" {
  description = "S3 bucket name for CAD files"
  value       = module.s3.cad_bucket_name
}

output "vpc_id" {
  value = module.vpc.vpc_id
}
