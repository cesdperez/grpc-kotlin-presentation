# GRPC Kotlin presentation

This project is based on https://github.com/hwslabs/grpc-kotlin-starter but I changed quite a few things. The later is also outdated, this one leverages Kotlin stubs and uses latest dependencies (at the moment it was created). 

For further Gradle protobuf plugin setup instructions, see https://github.com/grpc/grpc-kotlin/blob/master/compiler/README.md

## Generate stubs

On each directory:
```
./gradlew generateProto
```

Or just:
```
./gradlew build
```

## Run

Server:
Run `barservice.BarServer.kt`

Client:
Run `barclient.BarClient.kt`