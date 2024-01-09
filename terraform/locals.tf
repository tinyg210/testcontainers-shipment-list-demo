locals {
  json_data  = file("./data.json")
  tf_data    = jsondecode(local.json_data)
  sns_topic_arn_prod = "arn:aws:sns:${var.aws_region}:${var.account_id}:${var.sns_topic_name}"
  sns_topic_arn_dev = "arn:aws:sns:${var.aws_region}:000000000000:${var.sns_topic_name}"
}