package com.tarsys.android.orm.enums;

/**
 * Created by tarsys on 9/10/15.
 */
public enum DBDataType
{
    None
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "void";

                    return returnValue;
                }
            },
    IntegerDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "integer";

                    return returnValue;
                }
            },
    LongDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "integer";

                    return returnValue;
                }
            },
    StringDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = String.format("varchar(%d)", fieldLength);

                    return returnValue;
                }
            },
    TextDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "text";

                    return returnValue;
                }
            },
    RealDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "real";

                    return returnValue;
                }
            },
    DateDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "integer";

                    return returnValue;
                }
            },
    EntityDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "entity";

                    return returnValue;
                }
            },
    EntityListDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "entitylist";

                    return returnValue;
                }
            },
    EnumDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "integer";

                    return returnValue;
                }
            },
    BooleanDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    String returnValue = "integer";

                    return returnValue;
                }
            };

    public abstract String SqlType(int fieldLength);

    public static DBDataType DataType(String val)
    {
        DBDataType returnValue = null;
        if (val.toLowerCase().contains("integer")) returnValue = DBDataType.LongDataType;
        if (val.toLowerCase().contains("varchar")) returnValue = DBDataType.StringDataType;
        if (val.toLowerCase().contains("text")) returnValue = DBDataType.TextDataType;
        if (val.toLowerCase().contains("real")) returnValue = DBDataType.RealDataType;

        return returnValue;

    }
}

