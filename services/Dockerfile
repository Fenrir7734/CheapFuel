FROM mcr.microsoft.com/dotnet/sdk:6.0
ENV PATH $PATH:/root/.dotnet/tools
COPY . /app
WORKDIR /app
RUN dotnet dev-certs https
RUN dotnet tool install --global dotnet-ef --version 6.0.7
RUN dotnet restore
RUN dotnet build
EXPOSE 80 443
RUN chmod +x ./entrypoint.sh
CMD /bin/bash ./entrypoint.sh