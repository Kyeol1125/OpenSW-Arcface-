# 기본 이미지 선택
FROM tensorflow/tensorflow:latest

# 작업 디렉토리 설정
WORKDIR /opt/ml/

# 필요한 라이브러리 설치
COPY requirements.txt /opt/ml
RUN python3 -m pip install --upgrade pip
RUN pip install -r requirements.txt

# 패키지 업데이트 및 필요한 패키지 설치
RUN apt-get update -qq && apt-get install -y -qq libgl1-mesa-glx

ENV PATH="/opt/ml:${PATH}"

# 추론과 관련된 스크립트와 serve 스크립트 추가

COPY sub/ /opt/ml/
# serve 스크립트 실행 권한 추가
RUN chmod +x /opt/ml/serve

# model_files 전체를 /opt/ml/model로 복사
COPY model_files/ /opt/ml/model/

# NVIDIA 설정
#ENV NVIDIA_VISIBLE_DEVICES all

# Flask 서버 실행 포트 설정
ENV FLASK_RUN_PORT 8080
RUN apt-get install -y nginx

# SageMaker 설정
ENV SAGEMAKER_PROGRAM /opt/ml/model/inference.py

# Flask 서버 실행
CMD ["/opt/ml/serve"]