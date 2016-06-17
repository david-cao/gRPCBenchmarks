if [ ! -d "app/build/generated/source/proto_lite" ]; then
    mkdir app/build/generated/source/proto_lite
    echo "No proto_lite directory found, making it"
fi

if [ ! -d "app/build/generated/source/proto_lite/main" ]; then
    mkdir app/build/generated/source/proto_lite/main
    echo "No proto_lite/main directory found, making it"
fi

for p in app/src/main/proto_lite/*.proto; do
    protoc --java_out=lite:app/build/generated/source/proto_lite/main/ --proto_path=app/src/main/proto_lite/ $p
done

echo "Finished generating proto java lite files."
