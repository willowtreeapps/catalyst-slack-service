#!/usr/bin/env bash
sed -i -e "s/<aws-account-id>/$AWS_ACCOUNT_ID/g" -i -e "s/<aws-region>/$AWS_REGION/g" -i -e "s/<environment>/$AWS_ENVIRONMENT/g" task-definition.json