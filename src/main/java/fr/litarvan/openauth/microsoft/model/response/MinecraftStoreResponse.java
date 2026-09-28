

















package fr.litarvan.openauth.microsoft.model.response;

public class MinecraftStoreResponse
{
    private final StoreProduct[] items;
    private final String signature;
    private final String keyId;

    public MinecraftStoreResponse(StoreProduct[] items, String signature, String keyId)
    {
        this.items = items;
        this.signature = signature;
        this.keyId = keyId;
    }

    public StoreProduct[] getItems()
    {
        return items;
    }

    public String getSignature()
    {
        return signature;
    }

    public String getKeyId()
    {
        return keyId;
    }

    public static class StoreProduct
    {
        private final String name;
        private final String signature;

        public StoreProduct(String name, String signature)
        {
            this.name = name;
            this.signature = signature;
        }

        public String getName()
        {
            return name;
        }

        public String getSignature()
        {
            return signature;
        }
    }
}
