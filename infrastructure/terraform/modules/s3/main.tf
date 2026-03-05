resource "aws_s3_bucket" "cad_files" {
  bucket = "${var.project}-${var.environment}-cad-files"
  tags   = { Name = "${var.project}-cad-files", Environment = var.environment }
}

resource "aws_s3_bucket_versioning" "cad_files" {
  bucket = aws_s3_bucket.cad_files.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cad_files" {
  bucket = aws_s3_bucket.cad_files.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "cad_files" {
  bucket                  = aws_s3_bucket.cad_files.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
