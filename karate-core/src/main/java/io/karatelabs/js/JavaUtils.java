package io.karatelabs.js;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JavaUtils {
   static final Object NOT_FOUND = new Object();
   private static final Object METHOD_MARKER = new Object();
   private static final ClassValue<Map<String, Object>> MEMBERS = new ClassValue<Map<String, Object>>() {
      protected Map<String, Object> computeValue(Class<?> type) {
         return new ConcurrentHashMap<>();
      }
   };
   private static final int MEMBERS_LIMIT = 1024;
   static final Object[] EMPTY = new Object[0];

   static String jsTypeName(Class<?> clazz) {
      if (clazz == null) {
         return "Object";
      } else if (Map.class.isAssignableFrom(clazz)) {
         return "Object";
      } else if (List.class.isAssignableFrom(clazz)) {
         return "Array";
      } else if (clazz.isArray()) {
         return "Array";
      } else if (clazz == String.class) {
         return "String";
      } else if (Number.class.isAssignableFrom(clazz)
         || clazz == int.class
         || clazz == double.class
         || clazz == long.class
         || clazz == float.class
         || clazz == short.class
         || clazz == byte.class) {
         return "Number";
      } else if (clazz == Boolean.class || clazz == boolean.class) {
         return "Boolean";
      } else if (clazz == Character.class || clazz == char.class) {
         return "String";
      } else {
         return Set.class.isAssignableFrom(clazz) ? "Set" : clazz.getSimpleName();
      }
   }

   static Object construct(Class<?> clazz, Object[] args) {
      Constructor<?> constructor = findConstructor(clazz, args);

      try {
         return constructor.newInstance(args);
      } catch (InvocationTargetException var6) {
         Throwable cause = var6.getCause();
         if (cause instanceof RuntimeException re) {
            throw re;
         } else if (cause instanceof Error err) {
            throw err;
         } else {
            throw new RuntimeException(cause == null ? var6.getMessage() : cause.getMessage(), cause);
         }
      } catch (IllegalArgumentException var7) {
         throw noMatchingConstructor(clazz, args);
      } catch (ReflectiveOperationException var8) {
         throw JsErrorException.typeError(jsTypeName(clazz) + " is not a constructor");
      }
   }

   private static JsErrorException noMatchingConstructor(Class<?> clazz, Object[] args) {
      StringBuilder sb = new StringBuilder();

      for (Object arg : args) {
         if (!sb.isEmpty()) {
            sb.append(", ");
         }

         sb.append(arg == null ? "null" : jsTypeName(arg.getClass()));
      }

      return JsErrorException.typeError(jsTypeName(clazz) + " has no constructor matching (" + sb + ")");
   }

   static Object invokeStatic(Class<?> clazz, String name, Object[] args) {
      Method method = findMethod(clazz, name, args);
      if (method == null) {
         throw JsErrorException.typeError("." + name + " is not a function (called on " + jsTypeName(clazz) + ")");
      } else {
         try {
            return invoke(null, method, args);
         } catch (InvocationTargetException var7) {
            Throwable cause = var7.getCause();
            if (cause instanceof RuntimeException re) {
               throw re;
            } else if (cause instanceof Error err) {
               throw err;
            } else {
               throw new RuntimeException(cause == null ? var7.getMessage() : cause.getMessage(), cause);
            }
         } catch (IllegalAccessException var8) {
            throw JsErrorException.typeError("." + name + " is not accessible (on " + jsTypeName(clazz) + ")");
         }
      }
   }

   static Object invoke(Object object, String name, Object[] args) {
      Method method = findMethod(object.getClass(), name, args);
      if (method == null) {
         throw JsErrorException.typeError("." + name + " is not a function (called on " + jsTypeName(object.getClass()) + ")");
      } else {
         try {
            return invoke(object, method, args);
         } catch (InvocationTargetException var7) {
            Throwable cause = var7.getCause();
            if (cause instanceof RuntimeException re) {
               throw re;
            } else if (cause instanceof Error err) {
               throw err;
            } else {
               throw new RuntimeException(cause == null ? var7.getMessage() : cause.getMessage(), cause);
            }
         } catch (IllegalAccessException var8) {
            throw JsErrorException.typeError("." + name + " is not accessible (on " + jsTypeName(object.getClass()) + ")");
         }
      }
   }

   static Object getStatic(Class<?> clazz, String name) {
      try {
         Field field = clazz.getField(name);
         return field.get(null);
      } catch (Exception var10) {
         if (clazz != null) {
            Method getter = findStaticGetter(clazz, name);
            if (getter != null) {
               try {
                  return getter.invoke(null, EMPTY);
               } catch (Exception var9) {
                  throw JsErrorException.typeError("." + name + " is not a function (called on " + jsTypeName(clazz) + ")");
               }
            }

            for (Method m : clazz.getMethods()) {
               if (m.getName().equals(name)) {
                  JavaType jc = new JavaType(clazz);
                  return jc.getMethod(name);
               }
            }

            for (Class<?> nested : clazz.getClasses()) {
               if (nested.getSimpleName().equals(name)) {
                  return new JavaType(nested);
               }
            }
         }

         throw JsErrorException.typeError("." + name + " is not a property (on " + jsTypeName(clazz) + ")");
      }
   }

   private static Method findStaticGetter(Class<?> clazz, String name) {
      String getterSuffix = name.substring(0, 1).toUpperCase() + name.substring(1);
      Method method = findStaticMethod(clazz, "get" + getterSuffix);
      if (method == null) {
         method = findStaticMethod(clazz, "is" + getterSuffix);
      }

      return method;
   }

   private static Method findStaticMethod(Class<?> clazz, String name) {
      for (Method method : clazz.getMethods()) {
         if (method.getName().equals(name) && Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0) {
            return method;
         }
      }

      return null;
   }

   static void setStatic(Class<?> clazz, String name, Object value) {
      try {
         Field field = clazz.getField(name);
         field.set(null, value);
      } catch (Exception var4) {
         throw JsErrorException.typeError("cannot set ." + name + " (on " + jsTypeName(clazz) + ")");
      }
   }

   private static Object resolveMember(Class<?> clazz, String name) {
      Map<String, Object> cache = MEMBERS.get(clazz);
      Object found = cache.get(name);
      if (found != null) {
         return found;
      } else {
         Object resolved = lookupMember(clazz, name);
         if (cache.size() < 1024) {
            cache.put(name, resolved);
         }

         return resolved;
      }
   }

   private static Object lookupMember(Class<?> clazz, String name) {
      Method getter = findGetter(clazz, name);
      if (getter != null) {
         return getter;
      } else {
         try {
            return clazz.getField(name);
         } catch (Exception var7) {
            for (Method m : clazz.getMethods()) {
               if (m.getName().equals(name)) {
                  return METHOD_MARKER;
               }
            }

            return NOT_FOUND;
         }
      }
   }

   static Object get(Object object, String name) {
      Object result = getOrNotFound(object, name);
      if (result == NOT_FOUND) {
         throw JsErrorException.typeError("no instance property: " + name);
      } else {
         return result;
      }
   }

   static Object getOrNotFound(Object object, String name) {
      Object member = resolveMember(object.getClass(), name);
      if (member == NOT_FOUND) {
         return NOT_FOUND;
      } else if (member == METHOD_MARKER) {
         return new JavaObject(object).getMethod(name);
      } else {
         try {
            return member instanceof Field field ? field.get(object) : ((Method)member).invoke(object, EMPTY);
         } catch (Exception var4) {
            throw JsErrorException.typeError("cannot get ." + name + " (on " + jsTypeName(object.getClass()) + ")");
         }
      }
   }

   static void set(Object object, String name, Object value) {
      String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
      Object[] args = new Object[]{value};

      try {
         Method method = findMethod(object.getClass(), setterName, args);
         if (method == null) {
            throw new RuntimeException("no such method: " + setterName);
         } else {
            method.invoke(object, args);
         }
      } catch (Exception var6) {
         throw JsErrorException.typeError("cannot set ." + name + " (on " + jsTypeName(object.getClass()) + ")");
      }
   }

   private static Class<?>[] paramTypes(Object[] args) {
      Class<?>[] paramTypes = new Class[args.length];

      for (int i = 0; i < args.length; i++) {
         Object arg = args[i];
         paramTypes[i] = arg == null ? Object.class : arg.getClass();
      }

      return paramTypes;
   }

   private static Object invoke(Object object, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
      if (method.isVarArgs()) {
         Class<?>[] paramTypes = method.getParameterTypes();
         int fixedCount = paramTypes.length - 1;
         Class<?> varArgArrayType = paramTypes[fixedCount];
         if (args.length == paramTypes.length && args[fixedCount] != null && varArgArrayType.isAssignableFrom(args[fixedCount].getClass())) {
            return method.invoke(object, args);
         } else {
            Object[] invokeArgs = new Object[paramTypes.length];

            for (int i = 0; i < fixedCount; i++) {
               invokeArgs[i] = args[i];
            }

            Class<?> componentType = varArgArrayType.getComponentType();
            int varArgCount = Math.max(0, args.length - fixedCount);
            Object varArgArray = Array.newInstance(componentType, varArgCount);

            for (int i = 0; i < varArgCount; i++) {
               Array.set(varArgArray, i, args[fixedCount + i]);
            }

            invokeArgs[fixedCount] = varArgArray;
            return method.invoke(object, invokeArgs);
         }
      } else {
         Class<?>[] paramTypes = method.getParameterTypes();
         if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1].equals(Object[].class)) {
            List<Object> argsList = new ArrayList<>();

            for (int i = 0; i < paramTypes.length - 1; i++) {
               argsList.add(args[i]);
            }

            List<Object> lastArg = new ArrayList<>();

            for (int i = paramTypes.length - 1; i < args.length; i++) {
               lastArg.add(args[i]);
            }

            argsList.add(lastArg.toArray());
            return method.invoke(object, argsList.toArray());
         } else {
            return method.invoke(object, args);
         }
      }
   }

   private static Method findGetter(Class<?> clazz, String name) {
      if (name.isEmpty()) {
         return null;
      } else {
         String getterSuffix = name.substring(0, 1).toUpperCase() + name.substring(1);
         Method method = findMethod(clazz, "get" + getterSuffix, EMPTY);
         if (method == null) {
            method = findMethod(clazz, "is" + getterSuffix, EMPTY);
         }

         return method;
      }
   }

   private static Constructor<?> findConstructor(Class<?> clazz, Object[] args) {
      if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()) && clazz.getConstructors().length != 0) {
         try {
            return clazz.getConstructor(paramTypes(args));
         } catch (Exception var8) {
            for (Constructor<?> constructor : clazz.getConstructors()) {
               Class<?>[] argTypes = constructor.getParameterTypes();
               if (match(argTypes, args)) {
                  return constructor;
               }
            }

            throw noMatchingConstructor(clazz, args);
         }
      } else {
         throw JsErrorException.typeError(jsTypeName(clazz) + " is not a constructor");
      }
   }

   private static Method findMethod(Class<?> clazz, String name, Object[] args) {
      Method method = findMethodDirect(clazz, name, args);
      if (method == null) {
         return null;
      } else {
         Class<?> declaringClass = method.getDeclaringClass();
         if (!Modifier.isPublic(declaringClass.getModifiers()) || isModuleRestricted(declaringClass)) {
            Method accessibleMethod = findAccessibleMethod(clazz, name, method.getParameterTypes());
            if (accessibleMethod != null) {
               return accessibleMethod;
            }
         }

         return method;
      }
   }

   private static Method findMethodDirect(Class<?> clazz, String name, Object[] args) {
      try {
         return clazz.getMethod(name, paramTypes(args));
      } catch (Exception var9) {
         for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
               Class<?>[] argTypes = method.getParameterTypes();
               if (match(argTypes, args)) {
                  return method;
               }
            }
         }

         return null;
      }
   }

   private static boolean isModuleRestricted(Class<?> clazz) {
      Module module = clazz.getModule();
      if (module != null && module.isNamed()) {
         String packageName = clazz.getPackageName();
         return !module.isExported(packageName, JavaUtils.class.getModule());
      } else {
         return false;
      }
   }

   private static Method findAccessibleMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
      for (Class<?> iface : clazz.getInterfaces()) {
         try {
            Method method = iface.getMethod(name, paramTypes);
            if (Modifier.isPublic(iface.getModifiers())) {
               return method;
            }
         } catch (NoSuchMethodException var10) {
         }
      }

      for (Class<?> superclass = clazz.getSuperclass(); superclass != null && superclass != Object.class; superclass = superclass.getSuperclass()) {
         if (Modifier.isPublic(superclass.getModifiers()) && !isModuleRestricted(superclass)) {
            try {
               return superclass.getMethod(name, paramTypes);
            } catch (NoSuchMethodException var11) {
            }
         }

         for (Class<?> iface : superclass.getInterfaces()) {
            try {
               Method method = iface.getMethod(name, paramTypes);
               if (Modifier.isPublic(iface.getModifiers())) {
                  return method;
               }
            } catch (NoSuchMethodException var9) {
            }
         }
      }

      return null;
   }

   private static boolean matchSingle(Class<?> type, Object arg) {
      if (arg == null) {
         return true;
      } else if (type.equals(int.class)) {
         return arg instanceof Integer;
      } else if (type.equals(double.class)) {
         return arg instanceof Number;
      } else if (!type.equals(long.class)) {
         if (type.equals(boolean.class)) {
            return arg instanceof Boolean;
         } else if (type.equals(byte.class)) {
            return arg instanceof Byte;
         } else if (type.equals(char.class)) {
            return arg instanceof Character;
         } else if (type.equals(float.class)) {
            return arg instanceof Number;
         } else {
            return type.equals(short.class) ? arg instanceof Short : type.isAssignableFrom(arg.getClass());
         }
      } else {
         return arg instanceof Integer || arg instanceof Long;
      }
   }

   private static boolean match(Class<?>[] types, Object[] args) {
      boolean lastIsArray = types.length > 0 && types[types.length - 1].isArray();

      for (int i = 0; i < types.length; i++) {
         if (i >= args.length) {
            return lastIsArray && i == types.length - 1;
         }

         Object arg = args[i];
         Class<?> argType = types[i];
         if (argType.isArray()) {
            if (!(arg instanceof List<?> list)) {
               if (arg != null && !argType.isAssignableFrom(arg.getClass()) && (i != types.length - 1 || !matchSingle(argType.getComponentType(), arg))) {
                  return false;
               }
            } else {
               Class<?> arrayType = argType.getComponentType();
               int count = list.size();
               Object result = Array.newInstance(arrayType, count);

               for (int j = 0; j < count; j++) {
                  Array.set(result, j, list.get(j));
               }

               args[i] = result;
            }

            if (i == types.length - 1) {
               Class<?> componentType = argType.getComponentType();

               for (int k = i + 1; k < args.length; k++) {
                  if (!matchSingle(componentType, args[k])) {
                     return false;
                  }
               }

               return true;
            }
         } else if (!matchSingle(argType, arg)) {
            return false;
         }
      }

      return types.length == args.length;
   }

   static Object toMap(Object object) {
      if (object == null) {
         return null;
      } else if (object instanceof Map<?, ?> rawMap) {
         Map<String, Object> map = (Map<String, Object>) rawMap;
         Map<String, Object> result = new LinkedHashMap<>();
         map.forEach((k, vx) -> result.put(k, toMap(vx)));
         return result;
      } else if (object instanceof List<?> list) {
         List<Object> result = new ArrayList<>(list.size());

         for (Object v : list) {
            result.add(toMap(v));
         }

         return result;
      } else if (!object.getClass().isArray()) {
         return !(object instanceof String) && !(object instanceof Number) && !(object instanceof Boolean) && !(object instanceof Character)
            ? beanToMap(object)
            : object;
      } else {
         int n = Array.getLength(object);
         List<Object> result = new ArrayList<>(n);

         for (int i = 0; i < n; i++) {
            result.add(toMap(Array.get(object, i)));
         }

         return result;
      }
   }

   private static Map<String, Object> beanToMap(Object object) {
      Map<String, Object> result = new LinkedHashMap<>();
      Class<?> clazz = object.getClass();

      for (Method m : clazz.getMethods()) {
         if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() != Object.class) {
            String name = m.getName();
            String prop;
            if (name.startsWith("get") && name.length() > 3 && !"getClass".equals(name)) {
               prop = decapitalize(name.substring(3));
            } else {
               if (!name.startsWith("is") || name.length() <= 2 || m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) {
                  continue;
               }

               prop = decapitalize(name.substring(2));
            }

            try {
               Object value = m.invoke(object);
               result.put(prop, value == object ? null : toMap(value));
            } catch (Exception var11) {
            }
         }
      }

      for (Field f : clazz.getFields()) {
         if (!Modifier.isStatic(f.getModifiers())) {
            String name = f.getName();
            if (!result.containsKey(name)) {
               try {
                  Object value = f.get(object);
                  result.put(name, value == object ? null : toMap(value));
               } catch (Exception var10) {
               }
            }
         }
      }

      return result;
   }

   private static String decapitalize(String s) {
      if (s.isEmpty()) {
         return s;
      } else {
         return s.length() > 1 && Character.isUpperCase(s.charAt(1)) && Character.isUpperCase(s.charAt(0))
            ? s
            : Character.toLowerCase(s.charAt(0)) + s.substring(1);
      }
   }

   static Object convertIfArray(Object o) {
      if (o instanceof byte[]) {
         return Terms.toJsValue(o);
      } else if (o != null && o.getClass().isArray()) {
         List<Object> list = new ArrayList<>();
         int count = Array.getLength(o);

         for (int i = 0; i < count; i++) {
            list.add(Array.get(o, i));
         }

         return list;
      } else {
         return o;
      }
   }
}
