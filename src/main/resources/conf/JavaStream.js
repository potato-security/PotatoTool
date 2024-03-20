
// let text = CryptoJS.enc.Hex.parse(decrypted).toString(CryptoJS.enc.Latin1)
// let buf = Uint8Array.from(text, x => x.charCodeAt())

////////////////////////////
// 工具类
////////////////////////////

/** 是否为null */
const isNull = (data) => data === null
/** 是否为undefined */
const isUndefined = (data) => data === undefined
/** 获取数据类型 */
const jsType = (data) => {
    if (isNull(data)) return 'null'
    if (isUndefined(data)) return 'undefined'
    return data.__proto__.constructor.name
}
/** 是否为数组 */
const isArray = (data) => jsType(data).endsWith('Array')
/** 是否为Uint8Array */
const isUint8Array = (data) => jsType(data) == 'Uint8Array'
/** 是否为字符串 */
const isString = (data) => jsType(data) == 'String'
/** 是否为数字 */
const isNumber = (data) => jsType(data) == 'Number'
/** 是否为方法 */
const isFunction = (data) => jsType(data) == 'Function'
/** 是否为未初始化的空值 */
const isNone = (data) => isNull(data) || isUndefined(data)
/** 是否为无内容的数据 */
const isEmpty = (data) => {
    if (isNull(data) || isUndefined(data))
        return true
    if ((isString(data) || isArray(data)) && data.length == 0)
        return true
    if (jsType(data) == 'Object' && Object.entries(data).length == 0)
        return true

    return false
}

/** 小写，只保留hex字符 */
const hex_normalize = (data) => data.toLowerCase().replace(/[^\da-f]/gi, '')
/** 创建指定长度的空数组 */
const nullArray = (length) => new Array(length).fill(null)
/** 获取字符的数字编码 */
const ord = (data) => data.charCodeAt()
/** 转为int */
const int = (data) => isNumber(data) ? data : parseInt(data, 16)
/** 转为16进制字符串 */
const hex = (data, len = 1) => hex_normalize(isString(data) ? data : data.toString(16).padStart(len, '0'))

/** 比较数值是否相等 */
const isNumberEqual = (number1, number2) => int(number1) == int(number2)
const isByteEqual = isNumberEqual
/** 比较Array是否相等 */
const isArrayEqual = (array1, array2) => {
    if (array1.length != array2.length) return false
    return array1.every((x, i) => x == array2[i])
}
/** 比较16进制字符串是否相等 */
const isHexEqual = (hex1, hex2) => hex_normalize(hex1) == hex_normalize(hex2)
/** 比较数据是否相等，两个参数必须为同一种类型 */
const isDataEqual = (data1, data2) => {
    if (jsType(data1) != jsType(data2)) return false
    if (isString(data1)) return isHexEqual(data1, data2)
    if (jsType(data1).endsWith('Array')) return isArrayEqual(data1, data2)
    if (isNumber(data1)) return isNumberEqual(data1, data2)
    console.error('未覆盖的数据类型', jsType(data1))
}

/** 16进制字符串转ArrayBuffer */
const hex2Uint8Array = data => {
    data = hex_normalize(data)
    let buf = nullArray(data.length / 2).map((_, i) => int(data.slice(i * 2, (i + 1) * 2)))
    buf = Uint8Array.from(buf)
    return buf
}

/** 16进制字符串或数字数组转ArrayBuffer */
const data2Uint8Array = (data) => {
    let array_slice
    if (isArray(data)) {
        array_slice = Uint8Array.from(data)
    } else if (isString(data)) {
        array_slice = hex2Uint8Array(data)
    }
    return array_slice
}

/** ArrayBuffer转16进制字符串 */
const array2Hex = (data) => Array.from(data).map(byte => hex(byte, 2)).join('')
/** ArrayBuffer转数字 */
const array2Int = (data) => int(array2Hex(Array.from(data)))
/** ArrayBuffer转unicode字符 */
const array2Utf8 = (data) => {
    data = Array.from(data2Uint8Array(data))
    let str_hex = array2Hex(data)
    let str_urlEncode = str_hex.replace(/([\w]{2})/gi, '%$1')
    return decodeURIComponent(str_urlEncode)
}

////////////////////////////
// 协议常量
////////////////////////////

// https://docs.oracle.com/en/java/javase/12/docs/api/java.base/java/io/ObjectStreamConstants.html
// https://nowjava.com/docs/java-api-1.6/java/io/ObjectStreamConstants.html


/** 流协议的基本常量 */
const StreamConstants = class StreamConstants {
    // Magic number that is written to the stream header.
    static STREAM_MAGIC = 0xACED

    // Version number that is written to the stream header.
    static STREAM_VERSION = 0x0005

    // static PROTOCOL_VERSION_1 = 1
    // static PROTOCOL_VERSION_2 = 2

    // Base index for handles
    static BASE_REFERENCE_IDX = 0x7E0000
}


/** 流类型代码 */
const TerminalCode = class TerminalCode {
    /** 第一个标记值 */
    static TC_BASE = 0x70
    /** 空 */
    static TC_NULL = 0x70
    /** 引用 */
    static TC_REFERENCE = 0x71
    /** 类描述 */
    static TC_CLASSDESC = 0x72
    /** 对象 */
    static TC_OBJECT = 0x73
    /** 字符串 */
    static TC_STRING = 0x74
    /** 数组 */
    static TC_ARRAY = 0x75
    /** 类 */
    static TC_CLASS = 0x76
    /** 块状可选的数据 */
    static TC_BLOCKDATA = 0x77
    /** 块数据结束 */
    static TC_ENDBLOCKDATA = 0x78
    /** 重置流内容 */
    static TC_RESET = 0x79
    /** 长块数据 */
    static TC_BLOCKDATALONG = 0x7A
    /** 异常 */
    static TC_EXCEPTION = 0x7B
    /** 长字符串 */
    static TC_LONGSTRING = 0x7C
    /** 代理类描述 */
    static TC_PROXYCLASSDESC = 0x7D
    /** 该标记表示最后一个标记值 */
    static TC_MAX = 0x7E
    /** 枚举 */
    static TC_ENUM = 0x7E

    // baseWireHandle: 0x7E0000, // 8257536
}

/**
 * 类描述标志
 */
const ClassDescFlags = class ClassDescFlags {
    static SC_WRITE_METHOD = 0x01
    static SC_BLOCK_DATA = 0x08
    static SC_SERIALIZABLE = 0x02
    static SC_EXTERNALIZABLE = 0x04
    static SC_ENUM = 0x10
}

/**
 * 类型定义字符
 */
const TypeCode = class TypeCode {
    // prim_typecode
    static TYPE_BYTE = ord('B')// `B'	// byte
    static TYPE_CHAR = ord('C')// `C'	// char
    static TYPE_DOUBLE = ord('D')// `D'	// double
    static TYPE_FLOAT = ord('F')// `F'	// float
    static TYPE_INTEGER = ord('I')// `I'	// integer
    static TYPE_LONG = ord('J')// `J'	// long
    static TYPE_SHORT = ord('S')// `S'	// short
    static TYPE_BOOLEAN = ord('Z')// `Z'	// boolean
    // obj_typecode
    static TYPE_ARRAY = ord('[')// `[`	// array
    static TYPE_OBJECT = ord('L')// `L'	// object
}

const FieldType = class FieldType {
    static BYTE = TypeCode.TYPE_BYTE
    static CHAR = TypeCode.TYPE_CHAR
    static DOUBLE = TypeCode.TYPE_DOUBLE
    static FLOAT = TypeCode.TYPE_FLOAT
    static INTEGER = TypeCode.TYPE_INTEGER
    static LONG = TypeCode.TYPE_LONG
    static SHORT = TypeCode.TYPE_SHORT
    static BOOLEAN = TypeCode.TYPE_BOOLEAN
    static ARRAY = TypeCode.TYPE_ARRAY
    static OBJECT = TypeCode.TYPE_OBJECT
}

/** Types of objects */
const ContentType = class ContentType {
    static INSTANCE = 'ContentType.INSTANCE'
    static CLASS = 'ContentType.CLASS'
    static ARRAY = 'ContentType.ARRAY'
    static STRING = 'ContentType.STRING'
    static ENUM = 'ContentType.ENUM'
    static CLASSDESC = 'ContentType.CLASSDESC'
    static BLOCKDATA = 'ContentType.BLOCKDATA'
    static EXCEPTIONSTATE = 'ContentType.EXCEPTIONSTATE'
}

/** 原始类型 */
const PrimitiveTypes = [
    TypeCode.TYPE_BYTE,
    TypeCode.TYPE_CHAR,
    TypeCode.TYPE_DOUBLE,
    TypeCode.TYPE_FLOAT,
    TypeCode.TYPE_INTEGER,
    TypeCode.TYPE_LONG,
    TypeCode.TYPE_SHORT,
    TypeCode.TYPE_BOOLEAN,
]

/** 对象类型 */
const ObjectTypes = [
    TypeCode.TYPE_ARRAY,
    TypeCode.TYPE_OBJECT
]

/** class数据的类型 */
const ClassDataType = class ClassDataType {
    static NOWRCLASS = 'ClassDataType.NOWRCLASS'
    static WRCLASS = 'ClassDataType.WRCLASS'
    static EXTERNAL_CONTENTS = 'ClassDataType.EXTERNAL_CONTENTS'
    static OBJECT_ANNOTATION = 'ClassDataType.OBJECT_ANNOTATION'
}

/** class描述数据的类型 */
const ClassDescType = class ClassDescType {
    /** 普通类 */
    static NORMALCLASS = 'ClassDescType.NORMALCLASS'
    /** 代理类 */
    static PROXYCLASS = 'ClassDescType.PROXYCLASS'
};

// 冻结对象，防止修改
Array.from([StreamConstants, TerminalCode, ClassDescFlags, TypeCode, FieldType, ContentType, PrimitiveTypes, ObjectTypes, ClassDataType, ClassDescType]).forEach(x => Object.freeze(x))

////////////////////////////
// java对象类
////////////////////////////

/**
 * 解析出的java对象
 */
class ParsedJavaContent extends Object {
    type = null
    is_exception = false
    handle = 0
    constructor() {
        super()
        // this.type = content_type
    }
    toString() {
        return `[ParseJavaObject 0x${hex(this.handle)} - ${this.type}]`
    }
    dump(indent = 0) {
        return '\t'.repeat(indent) + this.toString()
    }
    toValue() {
        return this.toString()
    }
    getValue() {
        // 防止递归输出
        if (this.toValueFlag) {
            return this.toString()
        }
        // if (this.is_reference) {
        //     this.toValueFlag = true
        // }
        this.toValueFlag = true
        let value = this.toValue()
        delete this.toValueFlag
        return value
    }
}

/**
 * java类描述
 */
class JavaClassDesc extends ParsedJavaContent {
    type = ContentType.CLASSDESC
    /** 类描述的类型 */
    class_type = null
    /** 类名称 */
    name = ''
    /** 序列化版本的UID */
    serial_version_uid = 0
    /** Description flags byte */
    desc_flags = 0
    /** 类的字段 */
    fields = []
    /** 内部类 */
    inner_classes = []
    /** 注释对象 */
    annotations = []
    /** 此类的父类 */
    super_class = null
    /** 此类是否为父类 */
    is_super_class = false
    /** 类的接口 */
    interfaces = []
    /** 类的常量 */
    enum_constants = []
    /** 是否为内部类 */
    is_inner_class = false
    /** 是否为本地内部类 */
    is_local_inner_class = false
    /** 是否为静态内部类 */
    is_static_member_class = false
    constructor(type) {
        super()
        this.class_type = type
    }
    toString() {
        return `[classdesc 0x${hex(this.handle)}: name ${this.name}, uid ${this.serial_version_uid}`
    }
    is_array_class() {
        return this.name.startsWith("[")
    }
    get_hierarchy(classes) {
        if (this.super_class)
            if (this.super_class.class_type == ClassDescType.PROXYCLASS)
                console.warn("Hit a proxy class in super class hierarchy")
            else
                this.super_class.get_hierarchy(classes)
        classes.push(this)
    }
    validate() {
        let serial_or_extern = ClassDescFlags.SC_SERIALIZABLE | ClassDescFlags.SC_EXTERNALIZABLE
        if ((this.desc_flags & serial_or_extern) == 0 && this.fields && this.fields.length)
            throw new Error("Non-serializable, non-externalizable class has fields")
        else if ((this.desc_flags & serial_or_extern) == serial_or_extern)
            throw new Error("Class is both serializable and externalizable")
        else if (this.desc_flags & ClassDescFlags.SC_ENUM) {
            if (this.fields || this.interfaces)
                throw new Error("Enums shouldn't implement interfaces or have non-constant fields")
        } else if (this.enum_constants && this.enum_constants.length)
            throw new Error("Non-enum classes shouldn't have enum constants")
        return true
    }
    data_type() {
        // 代理类没有这个类型
        if (this.desc_flags == 0)
            return ''
        // 见classdata的定义 https://docs.oracle.com/javase/8/docs/platform/serialization/spec/protocol.html#a10258
        if (ClassDescFlags.SC_SERIALIZABLE & this.desc_flags)
            return ClassDescFlags.SC_WRITE_METHOD & this.desc_flags ? ClassDataType.WRCLASS : ClassDataType.NOWRCLASS
        if (ClassDescFlags.SC_EXTERNALIZABLE & this.desc_flags)
            return ClassDescFlags.SC_BLOCK_DATA & this.desc_flags ? ClassDataType.OBJECT_ANNOTATION : ClassDataType.EXTERNAL_CONTENTS
        throw new Error("Unhandled Class Data Type")
    }
    toValue() {
        return [this.name, this.fields.map(item => item.getValue()), this.super_class && this.super_class.getValue() || null]
    }
}

/** Java字符串类 */
class JavaString extends ParsedJavaContent {
    type = ContentType.STRING
    handle = 0
    value = ''
    length = 0
    constructor(data, handle) {
        super()
        this.handle = handle
        this.value = data // decode_modified_utf8(data)
        this.length = this.value.length
    }
    toString() {
        return `[String 0x${hex(this.handle)}: ${this.toValue()}]`
    }
    toValue() {
        return this.value
    }
}

/** java类 */
class JavaClass extends ParsedJavaContent {
    type = ContentType.CLASS
    constructor(handle, data) {
        super()
        this.handle = handle
        this.classdesc = data
        this.name = this.classdesc.name
    }
    toString() {
        return `[class 0x${hex(this.handle)}: ${this.classdesc}]`
    }
    toValue() {
        return [this.name, this.classdesc.getValue()]
    }
}

/** 块数据 */
class BlockData extends ParsedJavaContent {
    type = ContentType.BLOCKDATA
    data = []
    constructor(data) {
        super()
        this.data = data || []
    }
    toString() {
        return `[blockdata 0x${hex(this.handle)}: ${this.data.length} bytes]`
    }
    toValue() {
        return this.data
    }
}

/** 枚举类 */
class JavaEnum extends ParsedJavaContent {
    type = ContentType.ENUM
    constructor(handle, class_desc, value) {
        super()
        this.handle = handle
        this.classdesc = class_desc
        this.value = value
    }

    toString() {
        return `[Enum 0x${hex(this.handler)}: ${this.value}]`
    }
    toValue() {
        return this.value
    }
}

/** 类字段数据 */
class JavaField extends ParsedJavaContent {
    /** 字段类型 */
    type = null
    /** 字段名称 */
    name = ''
    /** 字段的值 */
    value = null
    /** 值所在的完整对象 */
    value_object = null
    /** 字段所在类名称 */
    class_name = ''
    /** 字段所在对象  */
    class_name_object = null

    constructor(field_type, name, class_name_object = null, class_name = '') {
        super()
        this.type = field_type
        this.name = name
        if (isString(class_name_object)) {
            this.class_name = class_name_object
        } else {
            // 优先从class_name_object取类名
            this.class_name = class_name
            this.class_name_object = class_name_object
            if (this.class_name_object instanceof JavaString) {
                this.class_name = class_name_object.value
            } else if (this.class_name_object instanceof JavaClassDesc) {
                this.class_name = class_name_object.name
            }
        }
        this.is_inner_class_reference = false // 未知其义

        this.validate(this.class_name)
    }

    toString() {
        return `[JavaField ${this.name}.${this.class_name}]`
    }

    validate(java_type) {
        if (this.type == FieldType.OBJECT) {
            if (!java_type)
                throw new Error("Class name can't be empty")
        }
        // if (this.class_name_object instanceof JavaString &&
        //     (java_type[0] != 'L' || !java_type.endsWith(';')))
        //     throw new Error("Invalid object field type:" + java_type)
    }

    toValue() {
        let value = this.value_object instanceof ParsedJavaContent ? this.value_object.getValue() : this.value_object
        return [this.name, value]
    }
}

/** 异常状态 */
class ExceptionState extends ParsedJavaContent {
    type = ContentType.EXCEPTIONSTATE

    constructor(field_type, exception_object, data) {
        super()
        this.exception_object = exception_object
        this.stream_data = data
        this.handle = exception_object.handle
    }

    toString() {
        return `[ExceptionState 0x${hex(this.handle)}]`
    }

    toValue() {
        return this.stream_data
    }
}

/** 用于指示已分析异常对象的异常 */
class ExceptionRead extends Error {
    type = null
    constructor(content) {
        super()
        this.exception_object = content
    }
    toValue() {
        return this.exception_object
    }
}

/** java数组 */
class JavaArray extends ParsedJavaContent {
    type = ContentType.ARRAY
    data = []

    constructor(handle, class_desc, field_type, content) {
        super()
        this.handle = handle
        this.classdesc = class_desc
        this.field_type = field_type
        this.data = Array.from(content)
    }
    toString() {
        return `[JavaArray 0x${hex(this.handle)} ${this.data.toString()}]`
    }
    toValue() {
        return this.data.map(v => v instanceof ParsedJavaContent ? v.getValue() : v)
    }
}

/** java实例对象 */
class JavaInstance extends ParsedJavaContent {
    type = ContentType.INSTANCE
    class_name = ''

    constructor(class_desc) {
        super()
        this.classdesc = class_desc
        this.class_name = class_desc.name
        this.field_data = {}
        this.annotations = class_desc.annotations
        this.is_external_instance = false
    }
    toString() {
        return `[JavaInstance 0x${hex(this.handle)} ${this.classdesc.name}]`
    }
    load_from_blockdata() {
        return false
    }
    load_from_instance() {
        return false
    }
    toValue() {
        // TODO: 需要重构
        // if (this.class_name == 'java.beans.beancontext.BeanContextChildSupport')
        //     debugger
        let forEach = (v) => {
            if (isEmpty(v))
                return v
            if (jsType(v) == 'JavaArray') {
                return v.getValue()
            } else if (isArray(v)) {
                return v.map(w => forEach(w))
            } else if (v instanceof ParsedJavaContent) {
                return v.getValue()
            } else if (jsType(v) == 'Object') {
                return Object.entries(v).map(([k, v]) => {
                    return [k, forEach(v)]
                })
            }
            return v
        }

        let result = [this.class_name]
        // // 父类注解
        // let annotations = this.classdesc?.super_class?.annotations ?? []
        // annotations.forEach((v) => {
        //     let k = this.class_name
        //     if (k in result) {
        //         result[k] = result[k].concat(forEach(v?.field_data))
        //     } else {
        //         result[k] = forEach(v?.field_data)
        //     }
        // })

        // 当前类注解
        result.push(forEach(this.annotations))
        // 字段数据
        result.push(Object.entries(this.field_data).reduce((o, [k, v]) => {
            o[k] = forEach(v)
            return o
        }, {}))

        return result
    }
}

//////////////////////////////////


/** java序列化字节流解析类 */
class JavaStream {
    /** java字节流 */
    buffer = null
    /** 字节流总长度 */
    buffer_length = 0
    /** 当前字节流读取位置 */
    cursor = 0
    /** 解析对象 */
    contents = []
    /** 引用数据 */
    reference = {}
    /** 保留引用记录 */
    reference_maps = []
    /** handle编号 */
    current_handle_id = StreamConstants.BASE_REFERENCE_IDX
    /** 数据类型和对应的处理方法 */
    type_code_handlers = {
        [TerminalCode.TC_NULL]: this.read_null.bind(this),
        [TerminalCode.TC_REFERENCE]: this.read_reference.bind(this),
        [TerminalCode.TC_CLASSDESC]: this.read_classdesc.bind(this),
        [TerminalCode.TC_OBJECT]: this.read_object.bind(this),
        [TerminalCode.TC_STRING]: this.read_new_string.bind(this),
        [TerminalCode.TC_ARRAY]: this.read_array.bind(this),
        [TerminalCode.TC_CLASS]: this.read_class.bind(this),
        [TerminalCode.TC_BLOCKDATA]: this.read_blockdata.bind(this),
        [TerminalCode.TC_BLOCKDATALONG]: this.read_blockdata.bind(this),
        // 无 [TerminalCode.TC_ENDBLOCKDATA]: this.read_blockdata,
        // 无 TC_RESET
        [TerminalCode.TC_EXCEPTION]: this.read_exception.bind(this),
        [TerminalCode.TC_LONGSTRING]: this.read_new_string.bind(this),
        [TerminalCode.TC_PROXYCLASSDESC]: this.read_classdesc.bind(this),
        [TerminalCode.TC_ENUM]: this.read_enum.bind(this),
        ['test']: this.test.bind(this),
    }
    test() {
        return this
    }
    /** 读取不同类型的字段 */
    field_type_handlers = {
        [FieldType.BYTE]: this.next_byte.bind(this),
        [FieldType.CHAR]: this.next_char.bind(this),
        [FieldType.DOUBLE]: this.next_double.bind(this),
        [FieldType.FLOAT]: this.next_float.bind(this),
        [FieldType.INTEGER]: this.next_int.bind(this),
        [FieldType.LONG]: this.next_long.bind(this),
        [FieldType.SHORT]: this.next_short.bind(this),
        [FieldType.BOOLEAN]: this.next_bool.bind(this),
        [FieldType.OBJECT]: this.next_object.bind(this),
        [FieldType.ARRAY]: this.next_array.bind(this),
    }

    constructor(buffer) {
        this.buffer = buffer
        this.buffer_length = buffer.length
    }

    toString() {
        return this.contents.toString()
    }

    /** 是否读取到末尾 */
    isEnd() {
        return this.cursor >= this.buffer_length
    }

    // 读取字节，不移动光标
    peek(length = 1) {
        let slice = this.buffer.slice(this.cursor, this.cursor + length)
        return slice
    }

    // 在数据流中移动光标，向后读取字节，默认为1个字节
    next(length = 1) {
        let slice = this.peek(length)
        this.cursor += length
        if (this.isEnd()) {
            console.info('已读取到末尾 cursor:', this.cursor)
            this.cursor = this.buffer_length
        }
        return slice
    }

    /** 读取一个字节 */
    peek_byte() {
        return this.peek()[0]
    }

    /** 读取有符号字节 */
    next_byte() {
        return new DataView(this.next(1).buffer).getInt8()
    }

    /** 读取无符号字节 */
    next_ubyte() {
        return new DataView(this.next(1).buffer).getUint8()
    }

    /** 读取布尔值 */
    next_bool() {
        return Boolean(this.next_byte())
    }

    /** 读取有符号短整形 */
    next_short() {
        return new DataView(this.next(2).buffer).getInt16()
    }

    /** 读取无符号短整形 */
    next_ushort() {
        return new DataView(this.next(2).buffer).getUint16()
    }

    /** 读取一个字符 */
    next_char() {
        return String.fromCharCode(this.next_short())
    }

    /** 读取有符号整形 */
    next_int() {
        return new DataView(this.next(4).buffer).getInt32()
    }

    /** 读取无符号整形 */
    next_uint() {
        return new DataView(this.next(4).buffer).getUint32()
    }

    /** 读取长整形 */
    next_long() {
        // https://stackoverflow.com/questions/53103695/how-to-read-64-bit-integer-from-an-arraybuffer-dataview-in-javascript
        return new DataView(this.next(8).buffer).getBigInt64()
    }

    /** 读取浮点数 */
    next_float() {
        return new Float32Array(this.next(4).buffer)[0]
    }

    /** 读取大浮点数 */
    next_double() {
        return new Float64Array(this.next(8).buffer)[0]
    }

    /** 读取字符串，支持unicode字符 */
    next_string(long = false) {
        let length = 0
        if (long) {
            length = this.next_long()
            if (length < 0 || length > 2 ** 31 - 1) //2147483647
                throw Error('ValueError("Invalid string length")')
            if (length < 65536)
                console.warn("Small string stored as a long one")
        } else {
            length = this.next_ushort()
        }
        // 12,e4b8ade69687e8bdace68da2
        let str_bytes = this.next(length)
        return array2Utf8(str_bytes)
    }

    next_object(type_code) {
        return this.read_field_value()
    }

    next_array(field_type) {
        if (isNone(type_code))
            field_type = this.next_byte()
        let sub_type_code = this.next_byte()
        if (field_type == FieldType.ARRAY) {
            if (sub_type_code == TerminalCode.TC_REFERENCE)
                return this.read_classdesc(sub_type_code)
            if (sub_type_code != TerminalCode.TC_ARRAY)
                throw new Error("Array type listed, but type code != TC_ARRAY")
        }
        return this.read_array(TerminalCode.TC_ARRAY)
    }

    /** 读取字节，并比较是否相等 */
    is_next_equal(data, length) {
        data = data2Uint8Array(data)
        let next_data = this.next(length || data.length)
        return isArrayBufferEqual(data, next_data)
    }

    /** 检查序列化标记 */
    checkMagic() {
        return this.next_ushort() == StreamConstants.STREAM_MAGIC
    }

    /** 检查版本号 */
    checkVersion() {
        return this.next_ushort() == StreamConstants.STREAM_VERSION
    }

    /** 重置引用表和handle id */
    reset() {
        if (this.reference)
            this.reference_maps.push(Object.assign({}, this.reference))

        this.reference = {}
        this.current_handle_id = StreamConstants.BASE_REFERENCE_IDX
    }

    /** 保存java对象映射表 */
    set_reference(handle, javaObj) {
        this.reference[handle] = javaObj
    }

    /** 获取下一个handle编号 */
    get_new_handle_id() {
        this.current_handle_id += 1
        return this.current_handle_id - 1
    }

    /** 返回null */
    read_null(type_code) {
        return null
    }

    /** 返回一个已经解析过的对象 */
    read_reference(type_code) {
        if (isNone(type_code))
            type_code = this.next_byte()
        let handle = this.next_int()
        let object = this.reference[handle]
        object.is_reference = true
        return object
    }

    /** 读取字段定义 */
    read_field() {
        /**
         primitiveDesc:
                prim_typecode fieldName
        objectDesc:
            obj_typecode fieldName className1
         */
        let field_type = this.next_byte(),
            field_name = this.next_string(),
            class_name_obj = null

        if (ObjectTypes.includes(field_type)) {
            // let str_type_code = this.next_byte()
            // if (str_type_code == TerminalCode.TC_REFERENCE) {
            //     class_name_obj = this.read_reference(str_type_code)
            // } else if ([TerminalCode.TC_STRING, TerminalCode.TC_LONGSTRING].includes(str_type_code)) {
            //     class_name_obj = this.read_new_string(str_type_code)
            // }
            class_name_obj = this.read_content()
        } else if (!PrimitiveTypes.includes(field_type)) {
            throw new Error('Invalid field type char: 0x' + hex(field_type))
        }

        let field = new JavaField(field_type, field_name, class_name_obj)
        console.log(`read_classdesc ${field_type} `, field)
        return field
    }

    /** 读取类描述 */
    read_classdesc(type_code) {
        /**
         * newClassDesc:
            TC_CLASSDESC className serialVersionUID newHandle classDescInfo
            TC_PROXYCLASSDESC newHandle proxyClassDescInfo
            classDescInfo:
                classDescFlags
                fields:
                    (short)<count>
                    fieldDesc[count]:
                        primitiveDesc:
                            prim_typecode fieldName
                        objectDesc:
                            obj_typecode fieldName className1
                classAnnotation:
                    endBlockData
                    contents endBlockData
                superClassDesc:
                    classDesc
         */


        // type_code 可选，可以提前读取再传入
        // 或由本方法自动读取
        let class_desc = null, handle
        if (this.isEnd())
            return class_desc

        if (isNone(type_code))
            type_code = this.next_byte()

        if (type_code == TerminalCode.TC_NULL)
            return null

        if (type_code == TerminalCode.TC_REFERENCE)
            return this.read_reference(type_code)

        if (type_code == TerminalCode.TC_CLASSDESC) {
            let name = this.next_string(),
                serial_version_uid = this.next_long(),
                // 进入classDescInfo
                desc_flags = this.next_byte(),
                // 有几个字段
                fields_count = this.next_short()
            handle = this.get_new_handle_id()
            if (fields_count < 0) {
                console.error('ValueError("Invalid field count")')
            }

            class_desc = new JavaClassDesc(ClassDescType.NORMALCLASS)
            class_desc.name = name
            // if (name == 'java.util.HashSet') debugger
            class_desc.serial_version_uid = serial_version_uid
            class_desc.handle = handle
            this.set_reference(handle, class_desc)
            // 读取字段信息
            class_desc.desc_flags = desc_flags
            class_desc.fields = nullArray(fields_count).map(_ => this.read_field())
            class_desc.annotations = this.read_class_annotations(class_desc)
            class_desc.super_class = this.read_classdesc()
            // 如果此类有父类，设置父类标记
            if (class_desc.super_class) {
                class_desc.super_class.is_super_class = true
                class_desc.annotations.unshift(...class_desc.super_class.annotations)
            }

        } else if (type_code == TerminalCode.TC_PROXYCLASSDESC) {
            let nb_interfaces = this.next_int()
            class_desc = new JavaClassDesc(ClassDescType.PROXYCLASS)
            handle = this.get_new_handle_id()
            class_desc.handle = handle
            this.set_reference(handle, class_desc)
            class_desc.interfaces = nullArray(nb_interfaces).map(_ => this.next_string())
            class_desc.annotations = this.read_class_annotations()
            class_desc.super_class = this.read_classdesc()
            if (class_desc.super_class) {
                class_desc.super_class.is_super_class = true
                class_desc.annotations.unshift(...class_desc.super_class.annotations)
            }
        }
        console.log(`read_classdesc ${type_code}`, class_desc)
        return class_desc
    }


    /** 读取对象 */
    read_object(type_code = 0) {
        if (isNone(type_code))
            type_code = this.next_byte()
        let class_desc = this.read_classdesc(),
            instance = this.create_instance(class_desc)
        instance.handle = this.get_new_handle_id()
        this.set_reference(instance.handle, instance)
        this.read_classdata(instance)
        // 代理类使用反射的名称用作类名
        if (class_desc && class_desc.super_class && class_desc.super_class.name == 'java.lang.reflect.Proxy') {
            let field = class_desc.super_class.fields[0]
            class_desc.name = instance.class_name = field && field.value_object && field.value_object.class_name || ''
        }
        // TODO: 实例类值按类格式转换成js中易读形式
        console.log(`read_object ${type_code}`, instance)
        return instance
    }

    /** 读取类实例字段的值 */
    read_classdata(instance) {
        // if (instance.class_name == 'java.beans.beancontext.BeanContextSupport')
        //     debugger
        if (this.isEnd())
            return instance
        let classes = [],
            all_data = {},
            annotations = []
        // 从父类开始读取数据
        instance.classdesc.get_hierarchy(classes)
        for (const cd of classes) {
            // 代理类没有class data
            if (cd.class_type == ClassDescType.PROXYCLASS)
                continue

            // let values = {}
            cd.validate()
            // if (cd.name in annotations) { debugger }
            if ([ClassDataType.NOWRCLASS, ClassDataType.WRCLASS].includes(cd.data_type())) {
                if (cd.data_type() == ClassDataType.WRCLASS && instance.is_external_instance)
                    // 读取注解
                    // annotations[cd.name] = this.read_class_annotations(cd)
                    annotations.push(...this.read_class_annotations(cd))
                else {
                    // 读取字段值
                    cd.fields.forEach(field => {
                        field.value_object = this.read_field_value(field.type)
                        field.value = field.value_object && field.value_object.value || field.value_object
                        // if (field.name in values) { debugger }
                        // 不用类名当key了
                        all_data[field.name] = field.value
                    })
                    // if (cd.name in all_data) { debugger }
                    // all_data[cd.name] = values
                    if (cd.data_type() == ClassDataType.WRCLASS)
                        // annotations[cd.name] = this.read_class_annotations(cd)
                        annotations.push(...this.read_class_annotations(cd))
                }
            } else {
                if (cd.data_type() == ClassDataType.OBJECT_ANNOTATION &&
                    !instance.load_from_blockdata(this, this.buffer))
                    // Call the transformer if possible .  Can't read :/
                    throw new Error(' "hit externalizable with nonzero SC_BLOCK_DATA; " "can\'t interpret data" ')
                // annotations[cd.name] = this.read_class_annotations(cd)
                annotations.push(...this.read_class_annotations(cd))
            }

            instance.annotations = annotations
            instance.field_data = all_data

            // Load transformation from the fields and annotations
            // instance.load_from_instance()
        }
        return instance
    }

    /** 创建java实例对象 */
    create_instance(class_desc) {
        // TODO:https://github.com/tcalmant/python-javaobj/blob/master/javaobj/v2/core.py#L496
        return new JavaInstance(class_desc)
    }

    /** 读取java字符串 */
    read_new_string(type_code) {
        if (isNone(type_code))
            type_code = this.next_byte()
        if (type_code == TerminalCode.TC_REFERENCE) {
            previous = this.read_reference(type_code)
            if (!(previous instanceof JavaString)) {
                console.error('ValueError("Invalid reference to a Java string")')
                return null
            }
            return previous
        }

        let handle = this.get_new_handle_id(),
            data = this.next_string(type_code == TerminalCode.TC_LONGSTRING),
            java_str = new JavaString(data, handle)
        this.set_reference(handle, java_str)
        console.log(`read_class ${type_code} `, java_str)
        return java_str
    }

    /** 读取数组 */
    read_array(type_code) {
        if (isNone(type_code))
            type_code = this.next_byte()
        // 这里也有可能是引用

        let cd = this.read_classdesc(),
            handle = this.get_new_handle_id(),
            arr = null
        if (!cd.name || cd.name.length < 2)
            console.error('ValueError("Invalid name in array class description")')

        // ParsedJavaContent type
        let field_type = ord(cd.name[1]),
            size = this.next_int()
        if (size < 0)
            throw new Error("Invalid array size")

        let content = []
        for (let index = 0; index < size; index++) {
            content.push(this.read_field_value(field_type))
        }
        arr = new JavaArray(handle, cd, field_type, content)
        this.set_reference(handle, arr)
        return arr
    }

    /** 读取类 */
    read_class(type_code) {
        /**
         * newClassDesc:
            TC_CLASSDESC className serialVersionUID newHandle classDescInfo
            TC_PROXYCLASSDESC newHandle proxyClassDescInfo
            classDescInfo:
                classDescFlags fields classAnnotation superClassDesc
         */
        if (isNone(type_code))
            type_code = this.next_byte()
        let cd = this.read_classdesc(),
            handle = this.get_new_handle_id(),
            class_obj = new JavaClass(handle, cd)
        this.set_reference(handle, class_obj)
        console.log(`read_class ${type_code} `, class_obj)
        return class_obj
    }

    /** 读取注释 */
    read_class_annotations(class_desc) {
        // http://www.akathink.com/2016/08/11/%E5%BD%BB%E5%BA%95%E6%90%9E%E6%87%82Java%E6%B3%A8%E8%A7%A3Annotation/
        let contents = []  // type: List[ParsedJavaContent]
        while (!this.isEnd()) {
            let type_code = this.next_byte()
            if (type_code == TerminalCode.TC_ENDBLOCKDATA)
                return contents
            else if (type_code == TerminalCode.TC_RESET) {
                this.reset()
                continue
            }

            let java_object = this.read_content(type_code, true, class_desc)

            if (java_object && java_object.is_exception)
                throw new ExceptionRead(java_object)

            contents.push(java_object)
        }
        return contents
    }

    /** 读取块数据 */
    read_blockdata(type_code) {
        if (isNone(type_code))
            type_code = this.next_byte()
        let size = 0
        if (type_code == TerminalCode.TC_BLOCKDATA)
            size = this.next_ubyte()
        else if (type_code == TerminalCode.TC_BLOCKDATALONG)
            size = this.next_int()
        else
            throw new Error('Invalid type code for blockdata')

        if (size < 0)
            throw new Error("Invalid value for block data size")

        let data = this.next(size)
        let blockdata = new BlockData(data)
        return blockdata
    }

    /** 读取异常数据 */
    read_exception(type_code) {
        this.reset()

        type_code = this.next_byte()
        if (type_code == TerminalCode.TC_RESET)
            throw new Error("TC_RESET read while reading exception")

        let content = this.read_content(type_code, False)
        if (!content)
            throw new Error("Null exception object")

        if (!content instanceof JavaInstance)
            throw new Error("Exception object is not an instance")

        if (content.is_exception)
            throw new Error(content)

        content.is_exception = True
        this.reset()
        return content
    }

    /** 读取枚举数据 */
    read_enum(type_code) {
        if (isNone(type_code))
            type_code = this.next_byte()
        let cd = this.read_classdesc()
        if (!cd)
            throw new Error("Enum description can't be null")

        let sub_type_code = this.next_byte(),
            enum_str = this.read_new_string(sub_type_code),
            handle = this.get_new_handle_id()

        cd.enum_constants.push(enum_str.value)
        let enum_obj = new JavaEnum(handle, cd, enum_str)
        this.set_reference(handle, enum_obj)
        return enum_obj
    }

    /** 读取内容块 */
    read_content(type_code, block_data, class_desc = null) {
        if (isNone(type_code))
            type_code = this.next_byte()
        if (!block_data && [TerminalCode.TC_BLOCKDATA, TerminalCode.TC_BLOCKDATALONG,].includes(type_code)) {
            throw new Error('ValueError("Got a block data, but not allowed here.")')
        }

        let handler = this.type_code_handlers[type_code]
        if (isFunction(handler)) {
            // debugger
            let value = handler(type_code)
            console.log(`read_content ${type_code} `, value)
            return value
        }
        else if (class_desc && class_desc.name && class_desc.data_type() == ClassDataType.WRCLASS)
            return this.read_custom_object(class_desc.name)
        else {
            throw new Error("Unknown type code: 0x{0:x}".format(type_code))
        }
    }

    /** 读取自定义对象 */
    read_custom_object(class_name) {
        // 光标回退一格
        this.cursor -= 1
        // for transformer in this.transformers:
        //     let class_data = transformer.load_custom_writeObject(
        //         this, this.buffer, class_name
        //     )
        if (class_data)
            return class_data

        throw new Error("Custom readObject can not be processed")
    }

    /** 读取字段值 */
    read_field_value(field_type) {
        if (isNone(field_type))
            field_type = this.next_byte()
        // field_type 是数字
        if ([FieldType.OBJECT, FieldType.ARRAY].includes(field_type)) {
            let sub_type_code = this.next_byte()
            if (field_type == FieldType.ARRAY) {
                if (sub_type_code == TerminalCode.TC_NULL)
                    return null
                if (sub_type_code == TerminalCode.TC_REFERENCE)
                    return this.read_classdesc(sub_type_code)
                if (sub_type_code != TerminalCode.TC_ARRAY)
                    throw new Error("Array type listed, but type code != TC_ARRAY")
            }

            let content = this.read_content(sub_type_code, false)
            if (content && content.is_exception)
                throw new Error(content)

            return content
        } else if (field_type in this.field_type_handlers)
            return this.field_type_handlers[field_type]()

        throw new Error(`Can't process type: ${field_type}`)
    }

    run() {
        // 先检查序列化标记和版本号
        if (!this.checkMagic()) {
            console.error('checkMagic ERROR')
            return
        }
        if (!this.checkVersion()) {
            console.error('checkVersion ERROR')
            return
        }

        // 循环读取Contents
        while (true) {
            // 读取TypeCode
            let startCursor = this.cursor,
                type_code = this.next_byte()

            if (type_code == TerminalCode.TC_RESET) {
                this.reset()
                continue
            }
            // debugger
            let parsed_content = this.read_content(type_code, true)

            if (parsed_content && parsed_content.is_exception) {
                // 回退光标，重新读取
                let endCursor = this.cursor
                this.cursor = startCursor
                stream_data = this.next(endCursor - startCursor)
                // Prepare an exception object
                parsed_content = new ExceptionState(parsed_content, stream_data)
            }

            this.contents.push(parsed_content)

            if (this.isEnd()) break
        }

        return this.contents
    }
}