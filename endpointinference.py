import boto3
import json

# boto3 클라이언트 설정
sagemaker_runtime_client = boto3.client('sagemaker-runtime', region_name='ap-northeast-2')

# 사용할 SageMaker 엔드포인트 이름
ENDPOINT_NAME = 'last'

# 추론할 이미지의 S3 경로
img1_path = 'upload/3.jpg'
img2_path = 'upload/4.jpeg'

# 이미지 경로를 JSON 형태로 변환
payload = {
    'img1_path': img1_path,
    'img2_path': img2_path
}

response = sagemaker_runtime_client.invoke_endpoint(
    EndpointName=ENDPOINT_NAME,
    ContentType='application/json',
    Body=json.dumps(payload)
)

# 추론 결과 출력
result = json.loads(response['Body'].read().decode())
print(result)
