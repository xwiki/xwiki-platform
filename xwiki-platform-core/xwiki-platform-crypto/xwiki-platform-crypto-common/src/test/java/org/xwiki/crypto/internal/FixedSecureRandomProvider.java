/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.crypto.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.bouncycastle.crypto.prng.FixedSecureRandom;
import org.bouncycastle.util.encoders.Base64;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

@Component
@Singleton
public class FixedSecureRandomProvider implements Provider<SecureRandom>, Initializable
{
    private static final byte[] RANDOM_DATA = Base64.decode(
        "YzP0/r4mlWCFXRFRTVXEtTUfxzFXF1PbfigDk1Zi77lkvCpv9de7UUw2DgsMggUAdchEUXl6kjJJ42ZFi0lBC11Kq6Y2sg"
        + "BJyJb2k2L9/9JTvzr2AGXGKVe3eMZ6vJrAmtuohFyyLwjz4dwME8k2fYCjoUenFw2EKPOJwtC19roEaFpA5Tn6O9YSSopz"
        + "LQm3JgiWbW/5+5mEgxplIkAZbQY0envDGTxwSeAaLdHsOThWFM7F4Lu2ch0cGOtDns34tALMWfdtODOtnnahfErmrI59AE"
        + "XykMDRXx9W9YlUa1+EA/vA3cXXUo5Uq+tL7lwo1ycxLzcBKZE1Mc6IHBFxOYuJY7lRMvLzykSLjhrG0G7/gyOxqpaPxp9Z"
        + "++4gQ+z1qkT8BAX+HYXqJEEmWXvUq6NRAzdzJJqYpckH+YffDDos4G9F5TE6agqWiASyOaAW0nsffTZtHEYVMwPhb0MsMX"
        + "NBSLXJeVjFP8WfE5M9zKsnIgKDR2iqWaDuUXhhdrkV5tOzEOz3iX+RtbHFH0hzD5j/4WQ8QtJgY1U6uRsHN6I+Vn9xmep3"
        + "l2P+xWn0MSLW2ova/Iis2zlTDkl5gFxFJi/gaTBQ1pMRMbpVSxGSjAmJydcp+zeJC8bVLCVpzCI9CW6JQu5MexUd/fuUQK"
        + "V07aZSdgxuyhjSSohf2fiuz1uhvLpea6a0z53uqju3HowBrEyH848T+4XQhd27paJkCHMVSj6BXb6nh8+ZIZ0DwiNlPTYa"
        + "zqbsPXAzrSfenrQ0YvInb09pJSDb7lQHEGNUESIJJRoAUFE2jVZSJXQMkaBursaefx3k2A2oFXHXdBtZTj2Yo7O26uCWya"
        + "pkMMCykDBvKZeFn6/IfliuXP9Sh07AmaP6l0rEZufRqPZj6yEzN0189C8h0hPoF9mmrSZMi9QuGfmQApBLj/GEFtv8yUGO"
        + "pYhRcoWBhxuAbMC7+dDnCLKJl6gwS8TWGVXW1kq8NlJHYHXzQON334M6Wv4NPxEftzIHOM8SXQLYNzQfF4Dnd2cqG/ObYk"
        + "eGar4ud1ujigiEmfephOzE+bB2YWxbFih346wtInk5OM+bK/f8r+J51762nnPlbOrKG0PkQoHLEbJAb8qCm+HUxXtG6su2"
        + "sXyXi+ZrVfupE0Xlz5tHZPWAdLjdSjbGt4Nbq7Tup2o4ldKSegQqbGX50YeYAG2N2MBMP/zOFKrHmnc2rgTUxNqFC3xghG"
        + "3BAvykdmHOqpW4Ts2d0zkmcxTUmPMYecw9+DLmK2qOTfI7DJL2v+gC4I81B3kBFuhdaHZwcintJwyetNQsyLWbYPpou+B+"
        + "eAVDFZ5zzErqSrj0IWLo+kEdYGjr3Ybm5oXX9j+/IDcYh1DFwr9FquOQyCfi1BUOTP4igy3cIhfwYgj2qbPOQdoY0Hk/KH"
        + "6v99i6572M5y47l/6psIBcqQ5pcW/OGWpxemSiHhNJrtsXE6ZqD+ZFM3Jy9VGUr8+B+yXsZybu0SSLTDz7uQ+krAM69rUZ"
        + "EKr+/6NGJ+jW4zSMcpvNnFaIExxfV//MTSnuYEPcKcOS1jD0eCg+wegqcAkfwJjE2wu4UVQW/jpdMIRdLsZ2l+JDsKHAyS"
        + "KJOA10l0deSvkS8B38vn+B4HHrsgwFtqtENSHkM5mv2tQv8ycqqo7ZuwZnLQhS44Q4JsXG0vko+CntJhrebWhIwUbbtSGt"
        + "5NvOvx+J/HW13RamRDi41PTuLgDFikbralfIbfAuwDA54/iAedBOypLhu9mCK+/KZONwdS86EBjP9LZoDN+8a4TXIw22c3"
        + "G/UQEFVGhdSX1L20MoZyuzjDIfRqKbowRvLhLiUuePY+JKvNswzF4wKUncL4YCO0HEVlxUMmHYj7SER4xPQJfQalZNVYuu"
        + "K35Qxg+1ZZfvpTTfKty0Cg89gLzn2rsmQgSNcw/3hJvwt5v/Sb66lvzc1cmWRmrJybcnamjQwPk7paIGwt6XX+uG1vNLiU"
        + "MO/fwGLalDgIyKYdvtkBxHzOxuHXa8L6LmKTtmhAhqMqDqXFVxs9lq9njsJ9hbeQpT6IL1zNTbVV7QTW5+YvWARtMtuQMf"
        + "gqEnwuNrMrJqmExRGZ+yoL/yklT6EWMTwvMsL1OfvHamaGJovdtdnXBcu/lr4iAjthPgnN7OuQzaUdwi6ObuVlo9potiQ6"
        + "Rph465/ogcWQCx4EzFw4H2aJlsI8tllIHfgvJHnibZ8hCC4VjyeB/afCFMFlqxyQeaQgqgEfRCRToz3ewM/g/ib3XJbXvX"
        + "vgue77la4fhXsJ/nF+KCSFIgvQErcco/9dN7k+DyMMC9wvMCRa0j39YzzflIhuL4THkbkEiRwYEWNjrlWQWDr9wflVv806"
        + "mhZqq7PSnUW3dh7Q9oUNX1X81aRuNGcbdtz5RT4XTWiwkzEsqjhQpx7H+zpp4wjeYg/itfwYAz2d0j9XwGO9f6tPevse6r"
        + "hmZfMTQwHltHcLhPdC4ajypTFdHti2urVoWfVgnZOe5PFOG/X7S7zyoyjxk7R477Z+BFOqvp7TyQMIk245CA4e3piY80Xo"
        + "78mhMPFK+7foL4QomngKQKDU8k5l3LQ5gjSRgQ6QONzTqTjPZuiNdleG/QHZI1ldyv4uwveMzKjev2Xvd/1OKC6Z0y8xN9"
        + "k1AJtLCUOMV48eMZlg9FvkDttcONqlqV/wUAhfgsR4j7G3LxPjlSLuxQNk+K105YJHYf+XfCLdCyqzsAhw4agspgGcKp9D"
        + "J0avqdb2qYWyjyPnV5oYMZjOckyejJiSsNehbsrb6XcGAcmcGXdLF/31Tr/LhZLidTVZQ0oIrm7cVLw1jaKa00/7OVuDz1"
        + "fzB+koZRl1RS+W9ugeXSZn0uEKTqbBbnADn4fmw9hOAF8bqZ0DPsgOirqrkO3PC0tquLrnbM1CTvCn9akHR9f+G60s2btC"
        + "UfGHXluPkrgoPe/kHTEmw8YciIg+wTt25xbguq4fFUa8uXmVwGbXYxQoobOMC3c5HzwyvKMCW6Ip0LYNNzc67dosjT8K/X"
        + "rhC1q0UKlfk1CYsyWn9yifuWXD+PjVokkJEHD1O8AXf8F2pWRitc31TEtDXd59yNZsFyPwLF9/2ceYFKfEuOfoyqBRANf/"
        + "/D6PLXjmuJ7RLyZdICscfsqO/RGKQBlRrbsuEJDIQJhAvO0uWQvcMm9LzE/KKNrVbmgjvQcOVRC56PhkvorMZO5ShNPGmM"
        + "qAfqg6hp9Q1N6Js58Dllo9hI+eFD1ILjEPQnr/JZRZobWgOsTZNXwEC80bWjMhgLbQfwlPHBHlhdIOOrbvWwdm729TYYYn"
        + "DdW+kOoXk0b3QfqWukAJhkurksEiQyPiEX4JSBNL2V4Bs/6/Wzkdtpwkl1fdeVLuegcZGdFEuVqYCBrX+ZFLKhQaywDJ9F"
        + "QhZPq1GamR6Ska+nZf7w2i8Gxl+Dw2wdr0tGJTCrQJy08Bo0ZcDHf82YICSDHOZ3ynf0G2Vq6JG++jpAJ6mZbqQUbXkHP/"
        + "dSaY/DQcf205awo3seIYu5nNNKnLRtE5j5G8AD2yKJXVe+K1bmgeJ5jauEVDQo5AyY4Vta2aZ0aVwu0co3iTJq2F3Jm9rp"
        + "ym4zO3rHsniMwlTadiqKxC1cjDQMbsAFidySTN6VGxaaYqD8Yx2ULePqWufNUHMtqx+BGewflDR8gOOoWaiZk2hj1w1sAg"
        + "8MBqeKbXToVLW2bLcNBKe2j814PxaSutatJi/5MO/BrQGwPTXr1OBaj36Ng+2q6HpbDVqf9pn5nURL16qifhVkuUKeVmrq"
        + "ISlc5ynY1SYH5AT+f+zftJ6BeIhR6rDRGZELhtFeR+7NWRma4RS0MRr5Gjvmou6zLJ0bGF3hEvjH4GfdXV4S/0iIqnG9l2"
        + "a8zND6V943KcV2ftD5NqgM9vg4R881An7CddFkT1rWGsugPxPW8/Zt2Mds4ElLU9ezoygy+en5/fRW7BkW4LzZg5ES624c"
        + "Dn/Objk0Xa0nA7TjITx+ekFDLrpkoZDo8mv9PNp1CRzGSCoMsk6kBpA3WGpCuiOLBT0jwDzPl1HS8tYO9QVYuyDknloTwS"
        + "EtnTOSCXpm0cD2L3kD+IGKuqjyvs5JjLe71ldPrx6XHxnPuIpunRmvhC/Nhp2legMOjx9TWsUznFFm5ag/2XuUMRd03Qnm"
        + "F8Q4fwWu1fgjW4ZVh/A6hKn9Wz1jVE9az5AtVtyWSbP536yGWzuBe9YkPpnU9yt9rN64COmcH8raY3g+MZkYPttbSWZG7V"
        + "4Tm3JMTZC054riYz8X1QVLHTmQZ9XA2QW5AehJeskhvy9yT2Y87NltOHdzXgQsyASiJUOzgEXaok6i6Or6VdxLnXLmUPCO"
        + "sK7De0lc6W0FHIr3ZSS8SvjSFVw3CxAjRcwEl5flKOpxEFiYmb8TD0NQ5FchOTtXBvQB8XEhqXhOJ5e+1ZLlALUV960bWr"
        + "5i1v/xdndOAt/njdDCQTisZ39uPx8OM1peuF+DvpoEHgqm/WIQc2flFIqgbg7pupoY7Exx22CtG2PLMehoWeDPYiPdLa9O"
        + "gG5ExS06ZVzhMHH0pP0SKrg1vwI9pPzpJ78j4OFhYudx5ZjqcHkAYey0adjZKvhkRAH4tp0IISgoLMN8loCkqLcQ3LqsdZ"
        + "YkAM20WWh0/kVQ8dYd+hdbuXlDz5kBu7kYXjjNAbHgzAQ3O5pzkNDGiNkneoxYDVtIRg50yW8ODJT0Bql6+hpKV2WJUbQS"
        + "2ZDdC7cJdl1pKViFVGKwzxVnGTfF7FV7GJUeVCTQINk9XJ7REbWyf2ybiRQLimJvbSFgoP6bMnrOsWsOSCNqKFGlfrSPJm"
        + "bTlVcrNUjd36HL+y1bSt4xBjL7R1TmsMz6LaJMwnWWbuCVIoW+K7DKgLIItlOTTYBKFU3iuYTt/WrpOi41SAOUsQpp0Yq1"
        + "64Ld8sCkAvhn5f2xTiKZ/Pd779uz1dTW6AGJ16DHGdwLnPWpessFWFcxN+qkqNIIK/cQemajIR5Bs7A4dEmdG5wgArmCDM"
        + "jNGlKSsM0YKzNS8sa3u0PMfSwvtw4uozcZKnESJm+fUle+11X7w/dhJoCrlkbz4L+0ZCIgXigAYPFYpEymaIvikHwhPYQb"
        + "JSVRpn9ojsai4nsDdjArzazaJU6BMlp/hTJ9q5BMhYnMNNNdTcf9sRFXZCuYqAdDqbfu3VjcZnFIjs/DsIJGkv8+V4ObQr"
        + "d1RbTEmMRQh63jC8rP0/l/xadrm29Dck/KkCYa1a4m0BdENuLYhMZVcsibEVzdI/0f+U8yj+wThVW0Qy9J/zfcn/QWYzKY"
        + "kfm9FONt1OSrsrdwD/eMnFIkh68aunN2KxvSqIwAvLD4Ew9E0raOHAvGujO04xkG9jA4BTc6Xbmo3HNCUKOkSj3MGGm6mM" //
        + "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");


    private SecureRandom random;

    @Override
    public SecureRandom get()
    {
        return random;
    }

    @Override
    public void initialize() throws InitializationException
    {
        //random = new MySecureRandom();
        random = new FixedSecureRandom(RANDOM_DATA);
    }

    public static class MySecureRandom extends SecureRandom
    {
        public int counter = 0;

        public ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public synchronized void nextBytes(byte[] bytes)
        {
            int o = counter;
            counter += bytes.length;
            if (o/10000 < counter/10000) System.out.println(counter);
            super.nextBytes(bytes);
            try {
                baos.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public byte[] generateSeed(int i)
        {
            int o = counter;
            counter += i;
            if (o/10000 < counter/10000) System.out.println(counter);
            byte[] b = super.generateSeed(i);
            try {
                baos.write(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return b;
        }
    }
}

/*
        FixedSecureRandomProvider rndprov =
            mocker.getInstance(new DefaultParameterizedType(null, Provider.class, SecureRandom.class));
        FixedSecureRandomProvider.MySecureRandom rnd = (FixedSecureRandomProvider.MySecureRandom) rndprov.get();

        while(rnd.counter > 4096) {
            rndprov.initialize();
            rnd = (FixedSecureRandomProvider.MySecureRandom) rndprov.get();
            //kp1 = generator.generate();
            //kp2 = generator.generate();
            KeyPair kp = generator.generate(new RSAKeyGenerationParameters(64));
        }

        assertThat(Base64.toBase64String(rnd.baos.toByteArray()), equalTo(""));*/
