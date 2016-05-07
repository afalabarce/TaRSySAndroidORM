package com.github.tarsys.android.orm.enums;

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
                    return"void";
                }
            },
    IntegerDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"integer";
                }
            },
    LongDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"integer";
                }
            },
    StringDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return String.format("varchar(%d)", fieldLength);
                }
            },
    TextDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"text";
                }
            },
    RealDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"real";
                }
            },
    DateDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"integer";
                }
            },
    EntityDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"entity";
                }
            },
    EntityListDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return"entitylist";
                }
            },
    EnumDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return "integer";
                }
            },
    BooleanDataType
            {
                @Override
                public String SqlType(int fieldLength)
                {
                    return "integer";
                }
            },
    Serializable {
        @Override
        public String SqlType(int fieldLength) {
            return "text";
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

